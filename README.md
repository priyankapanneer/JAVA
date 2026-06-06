CSV Data Analyzer

Overview

A simple pure-Java CLI application that reads an `employees.csv` file and computes Total Employees, Average Salary, Highest Salary, and Lowest Salary. No external libraries or databases are used.

Project layout

- src/main/java/com/example/csvanalyzer/*.java
- employees.csv

Compiling and running (VS Code integrated terminal)

1. Open the workspace folder `d:/java/csv-data-analyzer` in VS Code.
2. In the terminal, compile the sources:

```powershell
javac -d out src/main/java/com/example/csvanalyzer/*.java
```

3. Copy or ensure `employees.csv` is in the current working directory (project root). Then run:

```powershell
java -cp out com.example.csvanalyzer.Main
```

Or provide a custom path to CSV:

```powershell
java -cp out com.example.csvanalyzer.Main path\to\employees.csv
```

Notes

- The reader skips the header row and ignores rows with malformed salary values (e.g., non-numeric salaries).
- Exception handling prints user-friendly error messages for I/O and unexpected errors.

Viewing output in the VS Code integrated terminal

1. Open the Terminal in VS Code (Terminal → New Terminal). Ensure the current directory is the project root `d:/java/csv-data-analyzer`.

2. (Optional) Inspect the CSV before running:

```powershell
Get-Content employees.csv
```

3. Compile (PowerShell — easiest by changing into the source folder):

```powershell
Set-Location src\main\java\com\example\csvanalyzer
javac -d ../../../../../../out *.java
Set-Location D:\java\csv-data-analyzer
```

4. Run the program and observe the printed analysis in the terminal:

```powershell
java -cp out com.example.csvanalyzer.Main
```

Expected output (example):

```
Total Employees: 6
Average Salary: 77833.33
Highest Salary: 103000.00
Lowest Salary: 54000.00
```

PowerShell alternative (single-line compile):

```powershell
javac -d out (Get-ChildItem src\main\java\com\example\csvanalyzer\*.java | Select-Object -ExpandProperty FullName)
```

If you see `No employee data available.` or no output, confirm `employees.csv` exists in the project root and contains data rows (not only the header).
