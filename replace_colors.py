import os
import re

ui_dir = r"D:\Projeck APK\app\src\main\java\com\islami\Aha\ui"

for root, dirs, files in os.walk(ui_dir):
    for file in files:
        if file.endswith(".kt"):
            filepath = os.path.join(root, file)
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()

            if 'Emerald' in content or 'Color.Black' in content or 'Color.White' in content:
                lines = content.split('\n')
                new_lines = []
                modified = False
                for line in lines:
                    if line.strip().startswith('import '):
                        new_lines.append(line)
                        continue
                    
                    # Replace standalone Emerald
                    if re.search(r'\bEmerald\b', line):
                        line = re.sub(r'\bEmerald\b', 'MaterialTheme.colorScheme.primary', line)
                        modified = True
                        
                    # Handle Color.Black -> MaterialTheme.colorScheme.onSurface
                    if re.search(r'\bColor\.Black\b', line):
                        line = re.sub(r'\bColor\.Black\b', 'MaterialTheme.colorScheme.onSurface', line)
                        modified = True

                    # Handle Color.White for backgrounds (usually surface or primary)
                    # We might skip Color.White for now as it's tricky (could be text on primary)
                    # but let's see.

                    new_lines.append(line)

                if modified:
                    # check if MaterialTheme is imported
                    out_content = '\n'.join(new_lines)
                    if 'androidx.compose.material3.MaterialTheme' not in out_content:
                        # insert after package
                        out_lines = out_content.split('\n')
                        for i, l in enumerate(out_lines):
                            if l.startswith('package '):
                                out_lines.insert(i+1, '\nimport androidx.compose.material3.MaterialTheme')
                                break
                        out_content = '\n'.join(out_lines)

                    with open(filepath, 'w', encoding='utf-8') as f:
                        f.write(out_content)
                    print(f"Updated {filepath}")
