package com.huawei.java.main;

public class TNode {   //对时刻建模
    public int time;
    public int centerCostLow;   //理论上可能最低的价格
    public int centerCostHigh;   //理论上可能最高的价格
    public int demandAll;       //当前时刻总需求
    public boolean centerMax5;   //是否定义为中心节点的前百分之五时刻
    public TNode(int time, int centerCostLow){
        this.time = time;
        this.centerCostLow = centerCostLow;
    }
}
