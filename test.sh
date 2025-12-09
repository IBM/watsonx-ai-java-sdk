 if git diff --name-only origin/main...HEAD | grep -q ".github/project.yml"; then
		echo "changed=true" 
else
		echo "changed=false"
fi
