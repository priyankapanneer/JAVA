package com.example.csvanalyzer;

import java.util.List;

public class Analyzer {
    public static void analyze(List<Employee> employees) {
        if (employees == null || employees.isEmpty()) {
            System.out.println("No employee data available.");
            return;
        }

        int total = employees.size();
        double sum = 0;
        double highest = Double.NEGATIVE_INFINITY;
        double lowest = Double.POSITIVE_INFINITY;

        for (Employee e : employees) {
            double s = e.getSalary();
            sum += s;
            if (s > highest)
                highest = s;
            if (s < lowest)
                lowest = s;
        }

        double average = sum / total;

        System.out.println("Total Employees: " + total);
        System.out.printf("Average Salary: %.2f\n", average);
        System.out.printf("Highest Salary: %.2f\n", highest);
        System.out.printf("Lowest Salary: %.2f\n", lowest);
    }
}
