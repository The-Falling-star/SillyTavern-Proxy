#!/bin/bash

# 定义GitHub API地址和临时文件
API_URL="https://api.github.com/repos/The-Falling-star/SillyTavern-Proxy/releases/latest"
TMP_FILE=$(mktemp)

# 获取最新版本信息
echo "正在获取最新版本信息..."
if ! curl -sSL "$API_URL" -o "$TMP_FILE"; then
    echo "错误：无法获取发布信息，请检查网络连接"
    exit 1
fi

# 使用grep+sed提取下载链接
assets=$(
    grep -o '"browser_download_url":\s*"[^"]*"' "$TMP_FILE" |
    sed -E 's/"browser_download_url":\s*"([^"]*)"/\1/'
)

# 遍历下载每个文件
echo "$assets" | while read -r url; do
    filename=$(basename "$url")

    # 特殊处理application.yml
    if [[ "$filename" == "application.yml" ]]; then
        if [[ -f "application.yml" ]]; then
            new_name="application.yml.$(date +%Y%m%d%H%M%S)"
            echo "检测到本地配置文件存在，将下载为: $new_name"
            curl -sSL "$url" -o "$new_name"
            continue
        fi
    fi

    echo "正在下载: $filename"
    curl -sSL "$url" -O
done

# 清理临时文件
rm "$TMP_FILE"

echo "所有文件下载完成！"
