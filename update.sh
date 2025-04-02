#!/bin/bash

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


