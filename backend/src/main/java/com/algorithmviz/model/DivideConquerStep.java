package com.algorithmviz.model;

import java.util.List;

public class DivideConquerStep {
    private List<TreeNode> tree;
    private String currentNodeId;
    private String phase;
    private String x;
    private String y;
    private String a;
    private String b;
    private String c;
    private String d;
    private int split;
    private String z2;
    private String z1;
    private String z0;
    private String result;
    private String formula;
    private String description;
    private int codeLine;
    private int depth;
    private int multiplications;
    private int additions;

    public static Builder builder() {
        return new Builder();
    }

    public static class TreeNode {
        private String id;
        private String parentId;
        private String label;
        private String x;
        private String y;
        private String result;
        private int depth;
        private String state;

        public TreeNode() {}

        public TreeNode(String id, String parentId, String label, String x, String y,
                        String result, int depth, String state) {
            this.id = id;
            this.parentId = parentId;
            this.label = label;
            this.x = x;
            this.y = y;
            this.result = result;
            this.depth = depth;
            this.state = state;
        }

        public String getId() {
            return id;
        }

        public String getParentId() {
            return parentId;
        }

        public String getLabel() {
            return label;
        }

        public String getX() {
            return x;
        }

        public String getY() {
            return y;
        }

        public String getResult() {
            return result;
        }

        public int getDepth() {
            return depth;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public void setResult(String result) {
            this.result = result;
        }
    }

    public static class Builder {
        private final DivideConquerStep s = new DivideConquerStep();

        public Builder tree(List<TreeNode> v) {
            s.tree = v;
            return this;
        }

        public Builder currentNodeId(String v) {
            s.currentNodeId = v;
            return this;
        }

        public Builder phase(String v) {
            s.phase = v;
            return this;
        }

        public Builder x(String v) {
            s.x = v;
            return this;
        }

        public Builder y(String v) {
            s.y = v;
            return this;
        }

        public Builder a(String v) {
            s.a = v;
            return this;
        }

        public Builder b(String v) {
            s.b = v;
            return this;
        }

        public Builder c(String v) {
            s.c = v;
            return this;
        }

        public Builder d(String v) {
            s.d = v;
            return this;
        }

        public Builder split(int v) {
            s.split = v;
            return this;
        }

        public Builder z2(String v) {
            s.z2 = v;
            return this;
        }

        public Builder z1(String v) {
            s.z1 = v;
            return this;
        }

        public Builder z0(String v) {
            s.z0 = v;
            return this;
        }

        public Builder result(String v) {
            s.result = v;
            return this;
        }

        public Builder formula(String v) {
            s.formula = v;
            return this;
        }

        public Builder description(String v) {
            s.description = v;
            return this;
        }

        public Builder codeLine(int v) {
            s.codeLine = v;
            return this;
        }

        public Builder depth(int v) {
            s.depth = v;
            return this;
        }

        public Builder multiplications(int v) {
            s.multiplications = v;
            return this;
        }

        public Builder additions(int v) {
            s.additions = v;
            return this;
        }

        public DivideConquerStep build() {
            return s;
        }
    }

    public List<TreeNode> getTree() {
        return tree;
    }

    public String getCurrentNodeId() {
        return currentNodeId;
    }

    public String getPhase() {
        return phase;
    }

    public String getX() {
        return x;
    }

    public String getY() {
        return y;
    }

    public String getA() {
        return a;
    }

    public String getB() {
        return b;
    }

    public String getC() {
        return c;
    }

    public String getD() {
        return d;
    }

    public int getSplit() {
        return split;
    }

    public String getZ2() {
        return z2;
    }

    public String getZ1() {
        return z1;
    }

    public String getZ0() {
        return z0;
    }

    public String getResult() {
        return result;
    }

    public String getFormula() {
        return formula;
    }

    public String getDescription() {
        return description;
    }

    public int getCodeLine() {
        return codeLine;
    }

    public int getDepth() {
        return depth;
    }

    public int getMultiplications() {
        return multiplications;
    }

    public int getAdditions() {
        return additions;
    }
}