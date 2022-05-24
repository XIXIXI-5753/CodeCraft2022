package com.huawei.java.main;


import java.util.ArrayList;

class EdgeNode implements Comparable<EdgeNode>{
    public String name; //边缘节点名字
    public int bandwidth; //边缘节点带宽
    public int bandwidthAvalible[];
    public ArrayList<Integer> avalibleClient;
    public ArrayList<Integer> max5 = new ArrayList<Integer>();
    public Boolean superEdge = false;
    public int price95 = 0;
    public double price = 0;
    //public int difference = 0;
    public EdgeNode(String name, int bandwidth, ArrayList<Integer> avalibleClient){
        this.name = name;
        this.bandwidth = bandwidth;
        this.avalibleClient = avalibleClient;
    }
    public EdgeNode(String name, int bandwidth){
        this.name = name;
        this.bandwidth = bandwidth;
    }

    @Override
    public int compareTo(EdgeNode o) {   //目前最好效果是先按带宽从大到小,再按度从小到大
        if(this.bandwidth < o.bandwidth) return 1;
        else if(this.bandwidth > o.bandwidth) return -1;
        else {
            return Integer.compare(this.avalibleClient.size(), o.avalibleClient.size());
        }

    }
}
