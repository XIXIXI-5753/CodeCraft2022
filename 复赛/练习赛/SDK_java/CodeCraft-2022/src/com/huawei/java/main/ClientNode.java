package com.huawei.java.main;

import java.util.ArrayList;

class ClientNode{
    public String name;          //节点名字
    public int[] demand;         //节点带宽需求,按时间排列
    public ArrayList<Integer> avalibleEdgeNode; //该可用边缘节点
    public ArrayList<Integer> avalibleEdgeNode2; //该可用边缘节点
    public ArrayList<Stream>[] demandStream; //每个时刻需要的流的带宽需求
    public ClientNode(String name, int[] demand, ArrayList<Integer> avalibleEdgeNode){
        this.name = name;
        this.demand = demand;
        this.avalibleEdgeNode = avalibleEdgeNode;
    }

    public ClientNode(String name, ArrayList<Integer> avalibleEdgeNode){
        this.name = name;
        this.avalibleEdgeNode = avalibleEdgeNode;
    }
}