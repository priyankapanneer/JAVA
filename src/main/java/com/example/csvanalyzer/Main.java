package com.example.csvanalyzer;

import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        String path = "employees.csv";
        if (args.length > 0) {
            path = args[0];
        }

        try {
            List<Employee> employees = CSVReader.readEmployees(path);
            Analyzer.analyze(employees);
        } catch (IOException e) {
            System.err.println("I/O error while reading file: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
        }
    }
}
