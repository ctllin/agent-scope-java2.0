# 1. 删除 .git 目录
rm -rf .git

# 2. 重新初始化
git init
git branch -M master          # 【关键】确保本地分支名为 main

# 3. 强制添加所有文件（包括 .gitignore 中排除的文件）并提交
# 清除缓存（不会删除本地文件）
git rm --cached -r .
git add  .                  # 【关键】-f 参数会无视 .gitignore 规则
git commit -m "Initial commit"

# 3. 关联远程仓库并强制推送
git remote add origin git@gitee.com:ctllin/agent-scope-java2.git
git push -f origin master


