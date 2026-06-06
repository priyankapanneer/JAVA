package com.example.csvanalyzer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CSVReader {
    public static List<Employee> readEmployees(String path) throws IOException {
        List<Employee> employees = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (first) { // skip header
                    first = false;
                    continue;
                }
                if (line.trim().isEmpty())
                    continue;
                String[] parts = line.split(",");
                if (parts.length < 3)
                    continue; // skip malformed
                String name = parts[0].trim();
                String department = parts[1].trim();
                double salary;
                try {
                    salary = Double.parseDouble(parts[2].trim());
                } catch (NumberFormatException e) {
                    // skip rows with invalid salary
                    continue;
                }
                employees.add(new Employee(name, department, salary));
            }
        }
        return employees;
    }
}
