#!/bin/bash
echo "Building conversation"

# Create directory structure and copy frontend dist
cd ./backend
rm -rf ./src/main/resources/public/*.js
rm -rf ./src/main/resources/public/*.css
cp -R ../frontend/dist/* ./src/main/resources/

# Create view directory and copy HTML files
mv ./src/main/resources/*.html ./src/main/resources/view

# Clean up - remove frontend/dist and backend/src/main/resources
rm -rf ../frontend/dist