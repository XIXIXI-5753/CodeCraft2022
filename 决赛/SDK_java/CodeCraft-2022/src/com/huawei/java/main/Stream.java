package com.huawei.java.main;


class Stream implements Comparable<Stream>{
    public String name;   //流的名字
    public int demand;    //流的大小
    public int source;    //流的来源（用户）
    public int destination = -1;    //流的卸载目标（边缘节点）
    public int time;      //流的时刻
    public int avalibleEdgeSize;   //流可卸载的边缘节点数量
    public int sort;               //流的种类
    public Stream(String name, int demand, int source, int time, int avalibleEdgeSize){
        this.name = name;
        this.demand = demand;
        this.source = source;
        this.time = time;
        this.avalibleEdgeSize = avalibleEdgeSize;
    }

    @Override
    public int compareTo(Stream o) {
        return Integer.compare(o.demand /o.avalibleEdgeSize, this.demand/this.avalibleEdgeSize);  //
    }
}