#!/bin/bash

# 脚本说明
echo "####################################################"
echo "# 一键部署应用脚本 (安卓 Termux/Ubuntu 环境 | Linux 环境)"
echo "####################################################"
echo ""
echo "请确保已在 Termux 中安装 Ubuntu 系统。"
echo ""

# --- 函数定义 ---

# 检查命令是否已安装
check_command() {
  command -v "$1" >/dev/null 2>&1
}

# 安装 Java 21
install_java() {
  echo "--- 检查 Java 21 ---"
  if check_command java && java -version 2>&1 | grep -q 'openjdk'; then
    echo "Java 21 已安装，跳过安装。"
  else
    echo "Java 21 未安装或版本不符，开始安装..."
    sudo apt update
    if ! sudo apt install -y openjdk-21-jdk; then
      echo "Java 21 安装失败，请检查错误信息并重试。"
      return 1 # 返回非零退出码表示失败
    fi
    echo "Java 21 安装完成。"
    java -version # 验证安装
  fi
}

# 安装 Redis
install_redis() {
  echo "--- 检查 Redis ---"
  if check_command redis-server && redis-cli ping > /dev/null 2>&1; then
    echo "Redis 已安装并运行，跳过安装。"
  else
    echo "Redis 未安装或未运行，开始安装..."
    sudo apt update
    if ! sudo apt install -y redis-server; then
      echo "Redis 安装失败，请检查错误信息并重试。"
      return 1 # 返回非零退出码表示失败
    fi
    echo "Redis 安装完成。"
    # 启动 Redis 服务 (通常安装后会自动启动，但保险起见可以显式启动)
    if ! redis-cli ping > /dev/null 2>&1; then
      echo "Redis 服务启动失败，请检查日志。"
      return 1
    fi
    echo "Redis 服务已启动。"
  fi
}

# 下载配置文件
download_config() {
  # 定义GitHub API地址和临时文件
  API_URL="https://api.github.com/repos/The-Falling-star/SillyTavern-Proxy/releases/latest"

  links=$(curl -s $API_URL | grep browser_download_url | cut -d'"' -f4)

  for url in $links; do
      echo $url
      # 特殊处理application.yml
          if [[ "$url" == *"application.yml"* ]]; then
              if [[ -f "application.yml" ]]; then
                  new_name="application.yml.latest"
                  echo "检测到本地配置文件存在，将下载为: $new_name"
                  curl -sSL "$url" -o "$new_name"
                  continue
              fi
          fi
      echo "正在下载: $url"
      curl -sSL -O -# $url
  done
  echo "所有文件下载完成！"
}

# 配置项目 (打开配置文件让用户编辑)
configure_project() {
  echo "--- 配置项目 ---"
  echo "请打开 application.yml 文件，按照教程配置 token 和 Redis 连接信息。"
  echo "你可以使用 vim 或 nano 等编辑器。"
  echo "例如，使用 vim 编辑命令: vim application.yml"
  echo "     使用 nano 编辑命令: nano application.yml"
  echo ""
  echo "编辑完成后，请保存并退出编辑器。"
  # 这里脚本暂停，等待用户手动编辑配置文件
  read -p "按下 Enter 键继续脚本..." dummy_var
}


# --- 主流程 ---

echo "开始部署应用..."

# 1. 安装 Java 21
if ! install_java; then
  echo "部署脚本终止。"
  exit 1
fi

echo ""

# 2. 安装 Redis
if ! install_redis; then
  echo "部署脚本终止。"
  exit 1
fi

echo ""

# 3. 下载配置文件
if ! download_config; then
  echo "部署脚本终止。"
  exit 1
fi

echo ""

# 4. 配置项目 (手动编辑)
configure_project

echo ""
echo "####################################################"
echo "# 应用部署配置完成！"
echo "# 请按照教程的后续步骤启动你的应用。"
echo "####################################################"

exit 0
