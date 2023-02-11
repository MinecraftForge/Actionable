package net.minecraftforge.actionable.annotation;

public enum StringType {
    WORD {
        @Override
        public String toString() {
            return "word";
        }
    },
    STRING {
        @Override
        public String toString() {
            return "string";
        }
    },
    GREEDY_STRING {
        @Override
        public String toString() {
            return "greedyString";
        }
    }
}
