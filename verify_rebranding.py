#!/usr/bin/env python3
import os
import sys

# Define legacy terms and their target replacements or clean state expectations
LEGACY_TERMS = [
    "JobHunter",
    "Sriand",
    "Sriand Drive",
    "SriandDrive",
    "SiriAndDrive",
    "Siri and Drive",
    "Siri & Drive"
]

def scan_files():
    print("==========================================================")
    print(" KANNA AI REBRANDING VERIFICATION ENGINE")
    print("==========================================================")
    print("Scanning codebase for legacy branding terms...")
    
    violations_found = 0
    scanned_files_count = 0
    
    # Traverse project directory
    for root, dirs, files in os.walk("."):
        # Skip build, gradle, and hidden directories
        if any(skip in root for skip in ["build", "node_modules", ".git", ".gradle", "gradle"]):
            continue
            
        for file in files:
            # Only scan source files, layouts, and resource strings
            if file.endswith((".kt", ".xml", ".json", ".properties", ".gradle", ".kts", ".md")):
                scanned_files_count += 1
                filepath = os.path.join(root, file)
                try:
                    with open(filepath, "r", encoding="utf-8", errors="ignore") as f:
                        lines = f.readlines()
                        for line_num, line in enumerate(lines, 1):
                            for term in LEGACY_TERMS:
                                # Case-insensitive check to be extremely thorough
                                if term.lower() in line.lower():
                                    # Skip self-match inside this verification script
                                    if "verify_rebranding.py" in filepath:
                                        continue
                                    print(f"❌ Violation: Found '{term}' in {filepath} (Line {line_num})")
                                    print(f"   Context: {line.strip()}")
                                    violations_found += 1
                except Exception as e:
                    # Ignore unreadable files
                    pass

    print("==========================================================")
    print(f"Scan complete. Analyzed {scanned_files_count} files.")
    if violations_found == 0:
        print("🎉 SUCCESS: Rebranding to 'Kanna AI' is 100% COMPLETE! No legacy strings found.")
        return True
    else:
        print(f"⚠️ FAILURE: Found {violations_found} legacy branding violations.")
        return False

if __name__ == "__main__":
    success = scan_files()
    sys.exit(0 if success else 1)
