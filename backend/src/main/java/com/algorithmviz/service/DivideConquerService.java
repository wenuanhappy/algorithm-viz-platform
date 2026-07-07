package com.algorithmviz.service;

import com.algorithmviz.model.DivideConquerStep;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Service
public class DivideConquerService {

    private int nodeSeq;
    private int multiplications;
    private int additions;

    public List<DivideConquerStep> generateSteps(String algorithm, String x, String y) {
        return switch (algorithm) {
            case "karatsuba" -> karatsubaSteps(x, y);
            default -> throw new IllegalArgumentException("Unknown divide conquer algorithm: " + algorithm);
        };
    }

    private List<DivideConquerStep> karatsubaSteps(String x, String y) {
        validateNumber(x);
        validateNumber(y);

        nodeSeq = 0;
        multiplications = 0;
        additions = 0;

        List<DivideConquerStep> steps = new ArrayList<>();
        List<DivideConquerStep.TreeNode> tree = new ArrayList<>();

        BigInteger result = karatsuba(
                stripLeadingZeros(x),
                stripLeadingZeros(y),
                null,
                0,
                "root",
                tree,
                steps
        );

        markAllFinished(tree);
        steps.add(buildStep(
                tree,
                null,
                "finish",
                x,
                y,
                "",
                "",
                "",
                "",
                0,
                "",
                "",
                "",
                result.toString(),
                "Karatsuba(x, y) = " + result,
                "✓ 分治大整数乘法完成，最终结果为 " + result,
                8,
                0
        ));

        return steps;
    }

    private BigInteger karatsuba(String x, String y, String parentId, int depth, String label,
                                 List<DivideConquerStep.TreeNode> tree,
                                 List<DivideConquerStep> steps) {
        x = stripLeadingZeros(x);
        y = stripLeadingZeros(y);

        String nodeId = "n" + (++nodeSeq);
        DivideConquerStep.TreeNode node = new DivideConquerStep.TreeNode(
                nodeId,
                parentId,
                label,
                x,
                y,
                null,
                depth,
                "current"
        );
        tree.add(node);
        setCurrent(tree, nodeId);

        steps.add(buildStep(
                tree,
                nodeId,
                "divide",
                x,
                y,
                "",
                "",
                "",
                "",
                0,
                "",
                "",
                "",
                "",
                "Karatsuba(" + x + ", " + y + ")",
                "进入递归节点：计算 " + x + " × " + y,
                1,
                depth
        ));

        if (x.length() <= 2 || y.length() <= 2) {
            BigInteger result = new BigInteger(x).multiply(new BigInteger(y));
            multiplications++;
            node.setResult(result.toString());
            node.setState("done");
            steps.add(buildStep(
                    tree,
                    nodeId,
                    "base",
                    x,
                    y,
                    "",
                    "",
                    "",
                    "",
                    0,
                    "",
                    "",
                    "",
                    result.toString(),
                    x + " × " + y + " = " + result,
                    "规模足够小，直接相乘：" + x + " × " + y + " = " + result,
                    2,
                    depth
            ));
            return result;
        }

        int n = Math.max(x.length(), y.length());
        if (n % 2 != 0) {
            n++;
        }

        x = leftPad(x, n);
        y = leftPad(y, n);

        int m = n / 2;

        String a = stripLeadingZeros(x.substring(0, n - m));
        String b = stripLeadingZeros(x.substring(n - m));
        String c = stripLeadingZeros(y.substring(0, n - m));
        String d = stripLeadingZeros(y.substring(n - m));

        additions += 2;
        steps.add(buildStep(
                tree,
                nodeId,
                "split",
                x,
                y,
                a,
                b,
                c,
                d,
                m,
                "",
                "",
                "",
                "",
                "x = a×10^" + m + " + b, y = c×10^" + m + " + d",
                "拆分数字：" + x + " = " + a + "×10^" + m + " + " + b
                        + "，" + y + " = " + c + "×10^" + m + " + " + d,
                3,
                depth
        ));

        BigInteger z2 = karatsuba(a, c, nodeId, depth + 1, "z2 = a×c", tree, steps);
        steps.add(buildStep(
                tree,
                nodeId,
                "z2",
                x,
                y,
                a,
                b,
                c,
                d,
                m,
                z2.toString(),
                "",
                "",
                "",
                "z2 = a×c = " + z2,
                "完成高位乘法 z2 = " + a + " × " + c + " = " + z2,
                4,
                depth
        ));

        BigInteger z0 = karatsuba(b, d, nodeId, depth + 1, "z0 = b×d", tree, steps);
        steps.add(buildStep(
                tree,
                nodeId,
                "z0",
                x,
                y,
                a,
                b,
                c,
                d,
                m,
                z2.toString(),
                "",
                z0.toString(),
                "",
                "z0 = b×d = " + z0,
                "完成低位乘法 z0 = " + b + " × " + d + " = " + z0,
                5,
                depth
        ));

        BigInteger aPlusB = new BigInteger(a).add(new BigInteger(b));
        BigInteger cPlusD = new BigInteger(c).add(new BigInteger(d));
        additions += 2;

        BigInteger zMid = karatsuba(
                aPlusB.toString(),
                cPlusD.toString(),
                nodeId,
                depth + 1,
                "mid = (a+b)(c+d)",
                tree,
                steps
        );

        BigInteger z1 = zMid.subtract(z2).subtract(z0);
        additions += 2;

        steps.add(buildStep(
                tree,
                nodeId,
                "z1",
                x,
                y,
                a,
                b,
                c,
                d,
                m,
                z2.toString(),
                z1.toString(),
                z0.toString(),
                "",
                "z1 = (a+b)(c+d) - z2 - z0 = " + z1,
                "计算交叉项 z1：" + zMid + " - " + z2 + " - " + z0 + " = " + z1,
                6,
                depth
        ));

        BigInteger tenPowM = BigInteger.TEN.pow(m);
        BigInteger result = z2.multiply(tenPowM.pow(2))
                .add(z1.multiply(tenPowM))
                .add(z0);
        additions += 2;

        node.setResult(result.toString());
        node.setState("done");

        steps.add(buildStep(
                tree,
                nodeId,
                "combine",
                x,
                y,
                a,
                b,
                c,
                d,
                m,
                z2.toString(),
                z1.toString(),
                z0.toString(),
                result.toString(),
                "result = z2×10^" + (2 * m) + " + z1×10^" + m + " + z0",
                "合并结果：" + z2 + "×10^" + (2 * m) + " + " + z1 + "×10^" + m + " + " + z0 + " = " + result,
                7,
                depth
        ));

        return result;
    }

    private DivideConquerStep buildStep(List<DivideConquerStep.TreeNode> tree,
                                        String currentNodeId,
                                        String phase,
                                        String x,
                                        String y,
                                        String a,
                                        String b,
                                        String c,
                                        String d,
                                        int split,
                                        String z2,
                                        String z1,
                                        String z0,
                                        String result,
                                        String formula,
                                        String description,
                                        int codeLine,
                                        int depth) {
        return DivideConquerStep.builder()
                .tree(copyTree(tree))
                .currentNodeId(currentNodeId)
                .phase(phase)
                .x(x)
                .y(y)
                .a(a)
                .b(b)
                .c(c)
                .d(d)
                .split(split)
                .z2(z2)
                .z1(z1)
                .z0(z0)
                .result(result)
                .formula(formula)
                .description(description)
                .codeLine(codeLine)
                .depth(depth)
                .multiplications(multiplications)
                .additions(additions)
                .build();
    }

    private List<DivideConquerStep.TreeNode> copyTree(List<DivideConquerStep.TreeNode> tree) {
        List<DivideConquerStep.TreeNode> copied = new ArrayList<>();
        for (DivideConquerStep.TreeNode n : tree) {
            copied.add(new DivideConquerStep.TreeNode(
                    n.getId(),
                    n.getParentId(),
                    n.getLabel(),
                    n.getX(),
                    n.getY(),
                    n.getResult(),
                    n.getDepth(),
                    n.getState()
            ));
        }
        return copied;
    }

    private void setCurrent(List<DivideConquerStep.TreeNode> tree, String currentId) {
        for (DivideConquerStep.TreeNode node : tree) {
            if (!"done".equals(node.getState())) {
                node.setState(node.getId().equals(currentId) ? "current" : "pending");
            }
        }
    }

    private void markAllFinished(List<DivideConquerStep.TreeNode> tree) {
        for (DivideConquerStep.TreeNode node : tree) {
            node.setState("done");
        }
    }

    private void validateNumber(String value) {
        if (value == null || !value.matches("\\d+")) {
            throw new IllegalArgumentException("Only non-negative integer strings are supported.");
        }
    }

    private String stripLeadingZeros(String value) {
        String stripped = value.replaceFirst("^0+(?!$)", "");
        return stripped.isBlank() ? "0" : stripped;
    }

    private String leftPad(String value, int length) {
        if (value.length() >= length) {
            return value;
        }
        return "0".repeat(length - value.length()) + value;
    }
}