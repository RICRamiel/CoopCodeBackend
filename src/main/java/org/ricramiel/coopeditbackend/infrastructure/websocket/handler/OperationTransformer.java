package org.ricramiel.coopeditbackend.infrastructure.websocket.handler;

public class OperationTransformer {
    public static Operation transform(Operation incomingOperation, Operation missingOperation) {
        Operation transformed = new Operation();
        transformed.setType(incomingOperation.getType());
        transformed.setId(incomingOperation.getId());
        transformed.setUserId(incomingOperation.getUserId());
        transformed.setServerVersion(incomingOperation.getServerVersion());
        transformed.setText(incomingOperation.getText());

        int newLength = getNewLength(incomingOperation, missingOperation);
        int newPos = getNewPos(incomingOperation, missingOperation);

        transformed.setLength(newLength);
        transformed.setPos(newPos);

        return transformed;
    }

    private static int getNewPos(Operation incomingOperation, Operation missingOperation) {
        int newPos = incomingOperation.getPos();

        if (missingOperation.getType().equals("insert")) {
            if (missingOperation.getPos() <= incomingOperation.getPos()) {
                newPos += missingOperation.getText().length();
            }
        } else if (missingOperation.getType().equals("delete")) {
            if (missingOperation.getPos() < incomingOperation.getPos()) {
                newPos = Math.max(missingOperation.getPos(), newPos - missingOperation.getLength());
            }
        }
        return newPos;
    }

    private static int getNewLength(Operation incomingOperation, Operation missingOperation) {
        if (missingOperation.getType().equals("insert")
                && incomingOperation.getType().equals("insert")
                || missingOperation.getType().equals("delete")
                && incomingOperation.getType().equals("insert")
        ){
            return incomingOperation.getLength();
        }

        if (missingOperation.getType().equals("insert")
                && incomingOperation.getType().equals("delete")
        ){
            if (missingOperation.getPos() > incomingOperation.getPos() &&
                    missingOperation.getPos() < incomingOperation.getPos() + incomingOperation.getLength()) {
                return incomingOperation.getLength() + missingOperation.getLength();
            }
        }

        if (missingOperation.getType().equals("delete")
                && incomingOperation.getType().equals("delete")
        ) {
            return incomingOperation.getLength() - getOverlappingLength(
                    incomingOperation.getPos(),
                    incomingOperation.getLength(),
                    missingOperation.getPos(),
                    missingOperation.getLength()
            );
        }

        return incomingOperation.getLength();
    }

    public static int getOverlappingLength(int pos1, int len1, int pos2, int len2) {
        // Определяем границы отрезков
        int end1 = pos1 + len1;
        int end2 = pos2 + len2;

        // Проверяем, есть ли пересечение
        if (pos1 >= end2 || pos2 >= end1) {
            // Отрезки не пересекаются - возвращаем полную длину первого
            return 0;
        } else {
            // Отрезки пересекаются - вычисляем длину без пересечения
            int overlapStart = Math.max(pos1, pos2);
            int overlapEnd = Math.min(end1, end2);

            return overlapEnd - overlapStart;
        }
    }
}