package org.ricramiel.coopeditbackend.infrastructure.websocket.handler;


import lombok.Data;

@Data
public class Operation {
    private String type; // "insert" или "delete"
    private int pos;
    private String text; // для insert
    private int length;  // для delete
    private int id;
    private String userId;
    private int serverVersion;
}