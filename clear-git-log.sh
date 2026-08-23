# 1. 删除 .git 目录
rm -rf .git

# 2. 重新初始化
git init
git add .
git commit -m "Initial commit"

# 3. 关联远程仓库并强制推送
git remote add origin git@github.com:ctllin/agent-scope-java2.0.git
git push -f origin master