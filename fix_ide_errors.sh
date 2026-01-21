#!/bin/bash

# ClockWise Project - IDE Error Fix Script
# This script helps resolve "cannot resolve symbol" errors in Android Studio

echo "=================================================="
echo "ClockWise Project - IDE Error Fix Script"
echo "=================================================="
echo ""

# Function to print colored output
print_step() {
    echo -e "\n→ $1"
}

print_success() {
    echo -e "✓ $1"
}

# Step 1: Clean the project
print_step "Step 1: Cleaning project build files..."
./gradlew clean
print_success "Project cleaned"

# Step 2: Delete IDE cache files
print_step "Step 2: Removing IDE cache files..."
rm -rf .idea/caches
rm -rf .idea/libraries
rm -rf .gradle
rm -rf app/build
rm -rf build
print_success "IDE cache files removed"

# Step 3: Rebuild the project
print_step "Step 3: Rebuilding project..."
./gradlew assembleDebug
if [ $? -eq 0 ]; then
    print_success "Project built successfully!"
else
    echo "✗ Build failed - check errors above"
    exit 1
fi

# Step 4: Check for actual errors
print_step "Step 4: Checking for compilation errors..."
ERROR_COUNT=$(./gradlew assembleDebug 2>&1 | grep -i "error:" | wc -l)

if [ $ERROR_COUNT -eq 0 ]; then
    print_success "No compilation errors found!"
else
    echo "✗ Found $ERROR_COUNT compilation errors"
fi

echo ""
echo "=================================================="
echo "Fix Complete!"
echo "=================================================="
echo ""
echo "Next steps:"
echo "1. In Android Studio: File → Invalidate Caches / Restart"
echo "2. After restart: File → Sync Project with Gradle Files"
echo "3. If errors persist: Close Android Studio and reopen the project"
echo ""
echo "The project builds successfully. Any remaining errors"
echo "are likely IDE display issues, not actual code problems."
echo ""
