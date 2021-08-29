package com.hzontal.tella_vault.filter;

public class Sort {
        public enum Direction {
            ASC,
            DESC;
        }
        public enum Type{
            DATE,
            NAME
        }

        public Direction direction;
        public Type type;
        public String property;
    }