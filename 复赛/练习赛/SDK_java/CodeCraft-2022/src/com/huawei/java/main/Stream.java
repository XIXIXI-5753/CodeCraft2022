package com.huawei.java.main;

class Stream implements Comparable<Stream>{
    public String name;
    public int demand;
    public int source;
    public int destination = -1;
    public int time;
    public int avalibleEdgeSize;
    public Stream(String name, int demand, int source, int time, int avalibleEdgeSize){
        this.name = name;
        this.demand = demand;
        this.source = source;
        this.time = time;
        this.avalibleEdgeSize = avalibleEdgeSize;
    }

    @Override
    public int compareTo(Stream o) {     //按照带宽除以度排序

/*		if(this.demand < o.demand){   //先带宽从大到小,再度从小到大
			return 1;
		}else if(this.demand > o.demand){
			return -1;
		}else{
			return Integer.compare(this.avalibleEdgeSize, o.avalibleEdgeSize);
		}*/

        return Integer.compare(o.demand /o.avalibleEdgeSize, this.demand/this.avalibleEdgeSize);
    }
}
