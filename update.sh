#!/bin/bash

# 设置仓库信息
USERNAME="The-Falling-star"
REPO_NAME="SillyTavern-Proxy"

# 设置配置文件名
CONFIG_FILE="application.yml"

# 获取 GitHub API URL
API_URL="https://api.github.com/repos/$USERNAME/$REPO_NAME/releases/latest"

# 使用 curl 和 jq 获取所有 assets 信息
assets=$(curl -s "$API_URL" | jq -r '.assets[] | {name, browser_download_url}')

# 检查是否成功获取 assets 信息
if [[ -z "$assets" ]]; then
  echo "错误：未能获取 assets 信息。请检查用户名和仓库名。"
  exit 1
fi

# 循环遍历 assets 信息并下载
echo "开始处理文件..."
while IFS= read -r asset; do
  # 从 JSON 字符串中提取 name 和 browser_download_url
  name=$(echo "$asset" | jq -r '.name')
  url=$(echo "$asset" | jq -r '.browser_download_url')

  # 检查是否为配置文件
  if [[ "$name" == "$CONFIG_FILE" ]]; then
    # 检查配置文件是否存在
    if [ -f "$CONFIG_FILE" ]; then
      echo "配置文件 $CONFIG_FILE 已存在，跳过下载。"
    else
      echo "配置文件 $CONFIG_FILE 不存在，开始下载。"
      curl -L -o "$CONFIG_FILE" "$url"
      if [ $? -eq 0 ]; then
        echo "下载完成：$CONFIG_FILE"
      else
        echo "下载失败：$CONFIG_FILE"
      fi
    fi
  else
    # 下载其他文件
    echo "正在下载：$name"
    curl -L -o "$name" "$url"
    if [ $? -eq 0 ]; then
      echo "下载完成：$name"
    else
      echo "下载失败：$name"
    fi
  fi
done <<< "$assets"

echo "脚本执行完毕。"
exit 0
