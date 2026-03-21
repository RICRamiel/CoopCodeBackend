package org.ricramiel.coopeditbackend.infrastructure.websocket.handler;

public class OperationApplier {
    public static String apply(String content, Operation op) {
        return switch (op.getType()) {
            case "insert" -> content.substring(0, op.getPos()) + op.getText() + content.substring(op.getPos());
            case "delete" -> content.substring(0, op.getPos()) + content.substring(op.getPos() + op.getLength());
            default -> content;
        };
    }
    public static String safeApply(String content, Operation op) {
        return switch (op.getType()) {
            case "insert" -> safeSubstring(content,0, op.getPos()) + op.getText() + safeSubstring(content, op.getPos());
            case "delete" -> safeSubstring(content, 0, op.getPos()) + safeSubstring(content, op.getPos() + op.getLength());
            default -> content;
        };
    }

    public static String safeSubstring(String str, int start, int end) {
        if (str == null) {
            return null;
        }

        // Обрабатываем отрицательные индексы
        start = Math.max(start, 0);
        end = Math.max(end, 0);

        // Корректируем индексы, если они выходят за границы строки
        start = Math.min(start, str.length());
        end = Math.min(end, str.length());

        // Если start > end, возвращаем пустую строку
        if (start >= end) {
            return "";
        }

        return str.substring(start, end);
    }

    public static String safeSubstring(String str, int start) {
        if (str == null) {
            return null;
        }

        // Обрабатываем отрицательные индексы
        start = Math.max(start, 0);

        // Корректируем индексы, если они выходят за границы строки
        start = Math.min(start, str.length());

        return str.substring(start);
    }
}