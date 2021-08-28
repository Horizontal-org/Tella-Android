package com.hzontal.tella_vault.filter;

public class Sort {
        public enum Direction {
            ASC,
            DESC;
        }

        public Direction direction;
        public String property;
    }