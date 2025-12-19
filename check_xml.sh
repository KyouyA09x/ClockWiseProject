#!/bin/bash
# Script to find XML parsing errors

echo "Checking all XML files for parsing errors..."
echo "=========================================="

ERROR_COUNT=0

# Find all XML files and validate them
find app/src/main/res -name "*.xml" | while read -r file; do
    if ! xmllint --noout "$file" 2>&1; then
        echo "ERROR in: $file"
        xmllint --noout "$file" 2>&1
        ((ERROR_COUNT++))
    fi
done

echo "=========================================="
echo "Validation complete!"

# Also check for files without proper closing tags
echo ""
echo "Checking for files without newline at end..."
find app/src/main/res -name "*.xml" -exec sh -c '
    if [ -n "$(tail -c 1 "$1")" ]; then
        echo "No newline at end: $1"
    fi
' _ {} \;

echo "Done!"
