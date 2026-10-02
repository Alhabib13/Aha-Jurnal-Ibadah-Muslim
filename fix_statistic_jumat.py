import re

file_path = r"D:\Projeck APK\app\src\main\java\com\islami\Aha\ui\statistic\StatisticViewModel.kt"

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

target_code = """
            val items = mutableListOf<PastDayHabitItem>()
            fardhuHabits.forEach { habit ->
                val key = "default_${habit.id}"
                items.add(PastDayHabitItem(
                    habitKey = key,
                    name = habit.name,
                    isCompleted = completedKeys.contains(key),
                    category = habit.category,
                    isSunnah = false
                ))
            }
"""

replacement_code = """
            val isJumatEnabled = UserPreferencesManager.isJumatEnabled.value
            val isFriday = runCatching {
                val parsed = java.time.LocalDate.parse(dateKey)
                parsed.dayOfWeek == java.time.DayOfWeek.FRIDAY
            }.getOrDefault(false)

            val items = mutableListOf<PastDayHabitItem>()
            fardhuHabits.forEach { habit ->
                val key = "default_${habit.id}"
                var displayTitle = habit.name
                if (isJumatEnabled && isFriday && habit.name == "Sholat Dzuhur") {
                    displayTitle = "Sholat Jumat"
                }
                
                items.add(PastDayHabitItem(
                    habitKey = key,
                    name = displayTitle,
                    isCompleted = completedKeys.contains(key),
                    category = habit.category,
                    isSunnah = false
                ))
            }
"""

if target_code.strip() in content:
    print("Found exact block, replacing...")
    content = content.replace(target_code.strip(), replacement_code.strip())
else:
    # Use regex
    print("Using regex replacement...")
    regex_pattern = r"val items = mutableListOf<PastDayHabitItem>\(\)\s+fardhuHabits\.forEach \{ habit ->\s+val key = \"default_\$\{habit\.id\}\"\s+items\.add\(PastDayHabitItem\(\s+habitKey = key,\s+name = habit\.name,\s+isCompleted = completedKeys\.contains\(key\),\s+category = habit\.category,\s+isSunnah = false\s+\)\)\s+\}"
    content = re.sub(regex_pattern, replacement_code.strip(), content)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("StatisticViewModel.kt patched successfully.")
