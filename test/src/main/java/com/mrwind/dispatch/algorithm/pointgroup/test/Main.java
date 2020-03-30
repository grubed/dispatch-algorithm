package com.mrwind.dispatch.algorithm.pointgroup.test;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mrwind.dispatch.algorithm.common.Point;
import com.mrwind.dispatch.algorithm.pointgroup.*;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Main {

    static List<Point> getRandomPoints(int count, float pointWeight) {
        ArrayList<Point> points = new ArrayList<>();

        float minLng = 120.07f;
        float maxLng = 120.4f;
        float minLat = 30.12f;
        float maxLat = 30.4f;
        for (int i = 0; i < count; ++i) {
            Point point = new Point();
            // 每单耗费时间(小时) 随机为 [0.1, 0.5)
            point.weight = pointWeight < 0 ? (float) (Math.random() * 0.4 + 0.1) : pointWeight;

            if (Math.random() > 0.5) {
                point.lng = (float) ((minLng + maxLng) / 2 + (1 - Math.random()) * (maxLng - minLng) * Math.random() * 0.4);
                point.lat = (float) ((minLat + maxLat) / 2 + (1 - Math.random()) * (maxLat - minLat) * Math.random() * 0.4);
            } else {
                point.lng = (float) (minLng + Math.random() * (maxLng - minLng));
                point.lat = (float) (minLat + Math.random() * (maxLat - minLat));
            }

            points.add(point);
        }
        return points;
    }

    static List<Point> getPointsFromFile(String path) throws Exception {
        ArrayList<Point> points = new ArrayList<>();
        String testPointsStr = FileUtils.readFileToString(new File(path), "utf8");
        JSONObject testJson = (JSONObject) JSONObject.parse(testPointsStr);
        JSONArray arr = testJson.getJSONArray("points");
        for (int i = 0; i < arr.size(); ++i) {
            JSONObject jsonObject = arr.getJSONObject(i);
            float weight = jsonObject.getFloatValue("weight");
            String id = jsonObject.getString("id");
            points.add(new Point(jsonObject.getFloat("lng"), jsonObject.getFloat("lat"), weight == 0 ? 0.2f : weight, id));
        }
        return points;
    }

    static float distanceRatio = 1.5f;
    static PointGroup pointGroup;

    static Response group(List<Point> points, int groupCount, float maxPointCount) {
        int pointCount = points.size();
        // 如果 minPointCount > maxPointCount / 2 则分割之后可能出现比 maxPointCount 大的包
        int minPointCount = Math.round(maxPointCount / 2);
        float minWeight = 3f;
        float maxWeight = 6f;
        float limitWeight = 6.6f;

        PointGroup.Builder builder = new PointGroup.Builder()
                .minPointCount(minPointCount)
                .maxPointCount((int) maxPointCount)
                // 超过 maxPointCount 后整体因子
                .overflowPointFactor(0.9f)
                // 0.001: 米转千米 0.033: 每小时 30 公里 distanceRatio 由于计算的是最小生成树的距离和 比实际距离小 所以保守乘上
                .distanceWeight(0.001f * 0.033f * distanceRatio)
                .minWeight(minWeight)
                // 一天工作最大时间(小时)
                .maxWeight(maxWeight)
                // 极限工作时长 允许一包在 maxWeight limitWeight 之间 且实在无法很好分割时使用
                .limitWeight(limitWeight)
                // 使用直角距离计算树的距离和
                .rightAngleTreeWeight(true)
                // 使用直角距离计算最小生成树
                .rightAngleMST(true)
                .points(points, PointGroup.COORDINATE_WGS84);

        // 下面相当于只限制单量 [minPointCount, maxPointCount] (需要每个点的 weight 为 1)
//                .distanceWeight(0)
//                .minWeight(minPointCount)
//                .maxWeight(maxPointCount);

        PointGroup pointGroup = builder.build();

        Response minResponse = null;
        int minGroupCount = Integer.MAX_VALUE;
        int maxTryCount = 10;
        for (int i = 0; i < maxTryCount; ++i) {
            System.out.println("group try " + i + " maxPointCount: " + maxPointCount + " minWeight: " + minWeight + " maxWeight: " + maxWeight + " limitWeight: " + limitWeight);

            long start = System.currentTimeMillis();
            Response response = pointGroup.group();
            System.out.println("time: " + (System.currentTimeMillis() - start));
            statistics(pointGroup, response, pointCount, minPointCount, Math.round(maxPointCount));
            System.out.println();

            if (response.groupedTrees.size() < minGroupCount) {
                minGroupCount = response.groupedTrees.size();
                minResponse = response;
            }
            if (minGroupCount <= groupCount) {
                break;
            }

            maxPointCount *= 1.03;
            minWeight *= 1.08;
            maxWeight *= 1.1;
            limitWeight *= 1.07;
            pointGroup = pointGroup.newBuilder()
                    .maxPointCount(Math.round(maxPointCount))
                    .minWeight(minWeight)
                    .maxWeight(maxWeight)
                    .limitWeight(limitWeight)
                    .build();
        }

        Main.pointGroup = pointGroup;


        return minResponse;
    }

    static void statistics(PointGroup pointGroup, Response response, int pointCount, int minPointCount, int maxPointCount) {

        List<TreeNode> groupedTrees = response.groupedTrees;

//        System.out.println("group node group");
//        for (TreeNode subTree : groupedTrees) {
//            System.out.println(subTree.toPreOrderTraversalString());
//        }
//        System.out.println();
        System.out.println("groupCount: " + groupedTrees.size());
        // 单量
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        float avg = ((float) pointCount) / groupedTrees.size();
        int ltMin = 0;
        int gtMax = 0;
        int ltHalfMax = 0;
        int halfMax = maxPointCount / 2;
        float variance = 0;
        int checkCount = 0;

        // 距离
        int minDistance = Integer.MAX_VALUE;
        int maxDistance = Integer.MIN_VALUE;
        float distanceTotal = 0;
        float distanceVariance = 0;

        // edgeWeight
        float minWeight = Float.MAX_EXPONENT;
        float maxWeight = Float.MIN_VALUE;
        float weightTotal = 0;
        float weightVariance = 0;

        for (TreeNode subTree : groupedTrees) {
            int nodeCount = subTree.nodeCount;
            float nodeWeight = subTree.nodeWeight;
            int treeWeight = subTree.edgeWeight;
            int distance = (int) (treeWeight * distanceRatio);

            checkCount += subTree.nodeCount;

            if (nodeCount < min) {
                min = nodeCount;
            }
            if (nodeCount > max) {
                max = nodeCount;
            }
            if (nodeCount < minPointCount) {
                ltMin++;
            }
            if (nodeCount < halfMax) {
                ltHalfMax++;
            }
            if (nodeCount > maxPointCount) {
                gtMax++;
            }
            variance += (nodeCount - avg) * (nodeCount - avg);

            if (distance < minDistance) {
                minDistance = distance;
            }
            if (distance > maxDistance) {
                maxDistance = distance;
            }
            distanceTotal += distance;

            float weight = pointGroup.getWeight(nodeCount, nodeWeight, treeWeight);
            if (weight < minWeight) {
                minWeight = weight;
            }
            if (weight > maxWeight) {
                maxWeight = weight;
            }
            weightTotal += weight;
        }

        float avgDistance = distanceTotal / groupedTrees.size();
        float avgWeight = weightTotal / groupedTrees.size();
        for (TreeNode subTree : groupedTrees) {
            int nodeCount = subTree.nodeCount;
            float nodeWeight = subTree.nodeWeight;
            int treeWeight = subTree.edgeWeight;
            int distance = (int) (treeWeight * distanceRatio);

            distanceVariance += (distance - avgDistance) * (distance - avgDistance);

            float weight = pointGroup.getWeight(nodeCount, nodeWeight, treeWeight);

            weightVariance += (weight - avgWeight) * (weight - avgWeight);
        }

        System.out.println("min: " + min + " max: " + max + " avg: " + avg +
                " ltMin:" + ltMin + " gtMax: " + gtMax + " ltHalfMax: " + ltHalfMax + " variance: " + variance + " checkCount: " + checkCount);
        System.out.println("minDistance: " + minDistance + " maxDistance: " + maxDistance +
                " avgDistance: " + avgDistance + " distanceVariance: " + distanceVariance);
        System.out.println("minWeight: " + minWeight + " maxWeight: " + maxWeight +
                " avgWeight: " + avgWeight + " weightVariance: " + weightVariance);
        assert checkCount == pointCount;
    }

    static void output(Response response, List<Point> points) {
        JSONObject jsonObject = new JSONObject();

        JSONArray pointsArray = new JSONArray();
        pointsArray.addAll(points);
        jsonObject.put("points", pointsArray);

        JSONArray treeArray = new JSONArray();
        for (int i = 0; i < response.mst.length; i++) {
            if (response.mst[i] == -1) {
                // null 表示生成树原始根节点
                treeArray.set(i, null);
            } else {
                treeArray.set(i, response.mst[i]);
            }
        }

        response.cutEdges.forEach(edge -> {
            // 负的表示断边
            treeArray.set(edge.dest, edge.src > 0 ? -edge.src : 0.1);
        });

        jsonObject.put("tree", treeArray);

        JSONObject treeMap = new JSONObject();
        response.groupedTrees.forEach(node -> {
            JSONObject nodeObj = new JSONObject();
            nodeObj.put("nodeWeight", node.nodeWeight);
            nodeObj.put("edgeWeight", node.edgeWeight);
            nodeObj.put("weight", pointGroup.getWeight(node.nodeCount, node.nodeWeight, node.edgeWeight));
            treeMap.put(String.valueOf(node.index), nodeObj);
        });
        jsonObject.put("treeMap", treeMap);

        File file = new File("../outputs/result.json");
        try {
            FileUtils.writeStringToFile(file, jsonObject.toJSONString(), "utf8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void test() throws Exception {

        List<Point> points = getRandomPoints(100, 0.2f);
//        List<Point> points = getPointsFromFile("../outputs/test1.json");

        Response response = group(points, 8, 25);
        output(response, points);
    }

    public static void main(String[] args) throws Exception {
//        testPrim();

//        for (int i = 0; i < 10; ++i) {
//            test();
//        }

        test();

    }

    static void testPrim() {
         /*
           2    3
        (0)--(1)--(2)
        |    / \   |
        6| 8/   \5 |7
        | /      \ |
        (3)-------(4)
             9          */
        // graph -1 代表无意义或不连通
        PrimMST t = new PrimMST();
        int[][] graph = new int[][]{
                {-1, 2, -1, 6, -1},
                {2, -1, 3, 8, 5},
                {-1, 3, -1, -1, 7},
                {6, 8, -1, -1, 9},
                {-1, 5, 7, 9, -1},
        };

        int[] mst = t.primMST(graph, 5, new Random().nextInt(5));

        t.printMST(mst, 5, graph);

        int[][] graph1 = new int[][]{
                {-1, 2, -1, 6, -1, -1},
                {2, -1, 3, 8, 5, 10},
                {-1, 3, -1, -1, 7, 6},
                {6, 8, -1, -1, 9, -1},
                {-1, 5, 7, 9, -1, 3},
                {-1, 10, 6, -1, 3, -1},

        };

        int[] mst1 = t.primMST(graph1, 6, new Random().nextInt(6));
        t.printMST(mst1, 6, graph1);
    }
}
