# 1. 彻底清除历史
rm -rf .git

# 2. 重新初始化
git init
git branch -M main

# 3. 清除缓存并强制添加
git rm --cached -r . 2>/dev/null  # 忽略不存在的报错
git add .

# 4. 检查是否有文件被添加
if git diff --cached --quiet; then
    echo "❌ 错误：没有任何文件被添加！请检查 .gitignore 或当前目录。"
    exit 1
fi

# 5. 提交并推送
git commit -m "Initial commit"
git remote add origin git@github.com:ctllin/agent-scope-java2.0.git
git push -f origin main