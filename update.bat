@echo off
setlocal

REM 设置仓库信息
set USERNAME=The-Falling-star
set REPO_NAME=SillyTavern-Proxy

REM 设置配置文件名
set CONFIG_FILE=application.yml

REM 构建 GitHub API URL
set API_URL=https://api.github.com/repos/%USERNAME%/%REPO_NAME%/releases/latest

REM 下载 jq (如果没有安装的话，这里假设下载到脚本目录，并解压)
if not exist jq-win64.exe (
    echo 正在下载 jq...
    curl -L -o jq.zip https://github.com/jqlang/jq/releases/latest/download/jq-win64.exe.zip
    if %ERRORLEVEL% neq 0 (
        echo 下载 jq 失败!
        exit /b 1
    )
    echo 正在解压 jq...
    powershell -command "Expand-Archive -Path jq.zip -DestinationPath ."
    if %ERRORLEVEL% neq 0 (
        echo 解压 jq 失败!
        exit /b 1
    )
    del jq.zip
    ren jq-win64.exe jq-win64.exe
)

REM 使用 curl 和 jq 获取所有 assets 信息
for /f "tokens=* delims=" %%a in ('curl -s %API_URL% ^| .\jq-win64.exe -r ".assets[] | {name, browser_download_url}"') do (
  set "asset=%%a"

  REM 从 JSON 字符串中提取 name 和 browser_download_url
  for /f "tokens=2 delims=:" %%b in ('echo %asset% ^| .\jq-win64.exe -r ".name"') do set "name=%%b"
  for /f "tokens=2 delims=:" %%b in ('echo %asset% ^| .\jq-win64.exe -r ".browser_download_url"') do set "url=%%b"

  REM 去除双引号
  set "name=%name:"=%"
  set "url=%url:"=%"

  REM 检查是否为配置文件
  if "%name%"=="%CONFIG_FILE%" (
    REM 检查配置文件是否存在
    if exist "%CONFIG_FILE%" (
      echo 配置文件 %CONFIG_FILE% 已存在，跳过下载。
    ) else (
      echo 配置文件 %CONFIG_FILE% 不存在，开始下载。
      curl -L -o "%CONFIG_FILE%" "%url%"
      if %ERRORLEVEL% neq 0 (
        echo 下载失败：%CONFIG_FILE%
      ) else (
        echo 下载完成：%CONFIG_FILE%
      )
    )
  ) else (
    REM 下载其他文件
    echo 正在下载：%name%
    curl -L -o "%name%" "%url%"
    if %ERRORLEVEL% neq 0 (
      echo 下载失败：%name%
    ) else (
      echo 下载完成：%name%
    )
  )
)

echo 脚本执行完毕。
endlocal
exit /b 0
