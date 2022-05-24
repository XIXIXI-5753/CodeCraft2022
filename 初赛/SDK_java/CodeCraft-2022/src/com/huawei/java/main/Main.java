package com.huawei.java.main;

import java.io.*;
import java.lang.*;
import java.util.*;

public class Main {

    public static ClientNode[] clientSet ;  //用户节点数组
    public static EdgeNode[] edgeNodeSet;   //边缘节点数组
    public static int edgeNodeSize;         //边缘节点数量
    public static int clientNodeSize;       //客户节点数量
    public static int T;                    //时间
    public static int[][] demandDyn;

    public static void main(String[] args) {
        String filepath1 = "/data/demand.csv";
        String filepath2 = "/data/qos.csv";
        String filepath3 = "/data/site_bandwidth.csv";


        ArrayList<List<String>> demand = getdata(filepath1);   //读取三个csv文件
        ArrayList<List<String>> site_bandwidth = getdata(filepath3);
        ArrayList<List<String>> qos = getdata(filepath2);

        String[] clientName = new String[demand.get(0).size() - 1];
        for(int i = 1; i < demand.get(0).size(); i++){
            //  System.out.println(demand.get(0).get(i));
            clientName[i-1] = demand.get(0).get(i);
        }

        String[] edgeName = new String[site_bandwidth.size() - 1];
        for(int i = 1; i < site_bandwidth.size(); i++){
            //   System.out.println(site_bandwidth.get(i).get(0));
            edgeName[i-1] = site_bandwidth.get(i).get(0);
        }

        String[] clientName1 = new String[demand.get(0).size() - 1];
        for(int i = 1; i < qos.get(0).size(); i++){
            // System.out.println(qos.get(0).get(i));
            clientName1[i-1] = qos.get(0).get(i);
        }

        String[] edgeName1 = new String[site_bandwidth.size() - 1];
        for(int i = 1; i < qos.size(); i++){
            // System.out.println(qos.get(i).get(0));
            edgeName1[i - 1] = qos.get(i).get(0);
        }

        int[] clientNameInd = new int[demand.get(0).size() - 1];
        for(int i = 0; i < demand.get(0).size() - 1; i++){
            for(int j = 0; j < demand.get(0).size() - 1; j++){
                if(clientName[i].equals(clientName1[j])){
                    clientNameInd[i] = j;
                    break;
                }
            }
        }
        for(int i = 0; i< clientNameInd.length; i++){
            System.out.println(clientNameInd[i]);
        }

        int[] edgeNameInd = new int[site_bandwidth.size() - 1];
        for(int i = 0; i < site_bandwidth.size() - 1; i++){
            for(int j = 0; j < site_bandwidth.size() - 1; j++){
                if(edgeName[i].equals(edgeName1[j])){
                    edgeNameInd[i] = j;
                    break;
                }
            }
        }

        for(int i = 0; i < edgeNameInd.length; i++){
            //System.out.println(edgeNameInd[i]);
        }
        int qos_constraint = getqos();       //读取ini配置文件

        edgeNodeSize = qos.size() - 1;                    //边缘节点个数
        T = demand.size() - 1;                            //时间长度
        clientNodeSize = demand.get(0).size() - 1;    //用户节点个数

        //System.out.println(edgeNodeSize + T + clientNodeSize);

        clientSet = new ClientNode[clientNodeSize];  //用户节点数组
        edgeNodeSet = new EdgeNode[edgeNodeSize]; //边缘节点数组






//		for(int i = 0; i < clientNodeSize; i++){
//			System.out.println(clientSet[i].name);
//			System.out.println(clientSet[i].demand[0]);
//			System.out.println(clientSet[i].demand[T - 1]);
//			System.out.println(clientSet[i].avalibleEdgeNode);
//			System.out.println();
//		}


        for(int i = 0; i < edgeNodeSize; i++){     //初始化边缘节点
            String name = site_bandwidth.get(i + 1).get(0);
            int bandwidth = Integer.parseInt(site_bandwidth.get(i + 1).get(1));
            int[] bandwidthA = new int[T];
            ArrayList<Integer> avalibleClient = new ArrayList<Integer>();
            for(int j = 0; j < clientNodeSize; j++){
                if(Integer.parseInt(qos.get(edgeNameInd[i]+1).get(clientNameInd[j]+1)) < qos_constraint){  //得到可用客户节点列表
                    avalibleClient.add(j);
                }
            }
            edgeNodeSet[i] = new EdgeNode(name, bandwidth, avalibleClient);
            edgeNodeSet[i].bandwidthAvalible = new int[T];
            for(int k = 0; k < T; k++){
                edgeNodeSet[i].bandwidthAvalible[k] = bandwidth;
            }
        }

        ArrayList<Integer> edgelist = sortEdge(edgeNodeSet);  //对边缘节点可服务用户数量由小到大进行排序
        ArrayList<Integer> edgelist2 = sortEdge2(edgeNodeSet);

        ArrayList<Integer> edgelist3 = sortEdge3(edgeNodeSet);  //度和带宽相乘
        for(int i = 0; i < clientNodeSize; i++){    //初始化用户节点数组
            int[] clientdemand = new int[T];
            for(int j = 0; j < T; j++){
                clientdemand[j] = Integer.parseInt(demand.get(j+1).get(i + 1));
            }
            ArrayList<Integer> avalibleEdgeNode = new ArrayList<Integer>();
            for(int m = 0; m < edgelist3.size(); m++){
                int j = edgelist3.get(m);
                if(Integer.parseInt(qos.get(edgeNameInd[j]+1).get(clientNameInd[i]+1)) < qos_constraint){
                    avalibleEdgeNode.add(j);
                }
            }
            clientSet[i] = new ClientNode(demand.get(0).get(i+1), clientdemand,avalibleEdgeNode);
        }




        int[][][] result = new int[clientNodeSize][edgeNodeSize][T];    //创造结果矩阵

        demandDyn = new int[clientNodeSize][T];       //客户节点仍然要的带宽 ,需动态维护
        for(int i = 0; i < clientNodeSize; i++){
            for(int t = 0; t < T; t++){
                demandDyn[i][t] = clientSet[i].demand[t];
            }
        }

        //现在需要对边缘节点根据可服务用户数量由小到大进行排序

        //按照剩余客户总需求从小大到对时间排序
        int[] demandAllT1 = new int[T];
        for(int t = 0; t < T; t++){
            int demandall = 0;
            for(int i = 0; i < clientNodeSize; i++){
                demandall += demandDyn[i][t];
            }
            demandAllT1[t] = demandall;
        }
        ArrayList<Integer> demandAllTList1 = sortT(demandAllT1);
        System.out.println(demandAllTList1.size());
        for(int i = 0; i < demandAllTList1.size(); i++){
            System.out.println(String.valueOf(demandAllTList1.get(i)) + ":" + demandAllT1[demandAllTList1.get(i)]);

        }
        //  System.out.println("");
        // System.out.println("");


        //每个都选最大的,选T/20
/*        for(int r = 0; r < T/20; r++){
            for(int m = 0; m < edgelist.size(); m++){
                int j = edgelist.get(m);
                int[] demandCompute = new int[T];
                for (int t = 0; t < T; t++) {
                    for (int k = 0; k < edgeNodeSet[j].avalibleClient.size(); k++) {
                        demandCompute[t] += demandDyn[edgeNodeSet[j].avalibleClient.get(k)][t];
                    }
                }
                int maxT = getmaxT(j, demandCompute);
                edgeNodeSet[j].max5.add(j); //维护max5字段

                //以下开始分配maxT时刻边缘节点带宽
                int Ind = 0;
                while (true) {

                    //System.out.println("ss");
                    int duge = 0;
                    for (int y = 0; y < edgeNodeSet[j].avalibleClient.size(); y++) {
                        duge += demandDyn[edgeNodeSet[j].avalibleClient.get(y)][maxT];
                    }
                    if (duge == 0) break;
                    Ind = Ind % edgeNodeSet[j].avalibleClient.size();
                    int clientInd = edgeNodeSet[j].avalibleClient.get(Ind);
                    int bandwidthforEveryClient = Math.max(edgeNodeSet[j].bandwidth/(edgeNodeSet[j].avalibleClient.size()*100),10);
                    //int bandwidthforEveryClient = Math.max(edgeNodeSet[j].bandwidth / ((edgeNodeSet[j].avalibleClient.size())*20) * (clientNodeSize - clientSet[Ind].avalibleEdgeNode.size()),1);  //将带宽分片
                    //int bandwidthforEveryClient = Math.max((clientNodeSize - clientSet[Ind].avalibleEdgeNode.size()),10);
                    if (demandDyn[clientInd][maxT] > bandwidthforEveryClient) {
                        int bandwidthget = Math.min(bandwidthforEveryClient, edgeNodeSet[j].bandwidthAvalible[maxT]);
                        demandDyn[clientInd][maxT] -= bandwidthget;
                        result[clientInd][j][maxT] += bandwidthget;
                        edgeNodeSet[j].bandwidthAvalible[maxT] -= bandwidthget;
                        if (edgeNodeSet[j].bandwidthAvalible[maxT] == 0) break;
                        Ind++;
                        continue;
                    } else {
                        if (demandDyn[clientInd][maxT] <= edgeNodeSet[j].bandwidthAvalible[maxT]) {
                            result[clientInd][j][maxT] += demandDyn[clientInd][maxT];
                            edgeNodeSet[j].bandwidthAvalible[maxT] -= demandDyn[clientInd][maxT];
                            demandDyn[clientInd][maxT] = 0;
                            Ind++;
                            continue;
                        } else {
                            result[clientInd][j][maxT] += edgeNodeSet[j].bandwidthAvalible[maxT];
                            demandDyn[clientInd][maxT] -= edgeNodeSet[j].bandwidthAvalible[maxT];
                            edgeNodeSet[j].bandwidthAvalible[maxT] = 0;
                            break;
                        }
                    }
                }
            }

        }*/


        int edgeEnd = 5;

        for (int m =  edgelist2.size() - 1; m >= edgeEnd; m--) {   //遍历边缘节点,找到需求(动态)最高的五个时刻,死循环跳出条件为 该边缘节点带宽分配完毕 或 该边缘节点可服务客户已无需求
            int j = edgelist2.get(m);
            int[] demandCompute = new int[T];
            int[] max5 = null;
            for (int t = 0; t < T; t++) {
                for (int k = 0; k < edgeNodeSet[j].avalibleClient.size(); k++) {
                    demandCompute[t] += demandDyn[edgeNodeSet[j].avalibleClient.get(k)][t];
                }
            }
            max5 = sortList(demandCompute, edgeNodeSet[j].bandwidth);
            for (int u = 0; u < max5.length; u++)
                edgeNodeSet[j].max5.add(max5[u]);   //维护边缘节点对象中max5字段

            //System.out.println(edgeNodeSet[j].name + String.valueOf(max5[0]) +" "+ String.valueOf(max5[1])+" "+ String.valueOf(max5[2])+" "+ String.valueOf(max5[3])+ " "+String.valueOf(max5[4]));
            for (int t = 0; t < max5.length; t++) {
                if (edgeNodeSet[j].avalibleClient.size() == 0) break;
                int Ind = 0;
                while (true) {

                    //System.out.println("ss");
                    int duge = 0;
                    for (int y = 0; y < edgeNodeSet[j].avalibleClient.size(); y++) {
                        duge += demandDyn[edgeNodeSet[j].avalibleClient.get(y)][max5[t]];
                    }
                    if (duge == 0) break;
                    Ind = Ind % edgeNodeSet[j].avalibleClient.size();
                    int clientInd = edgeNodeSet[j].avalibleClient.get(Ind);
                    //int bandwidthforEveryClient = Math.max(edgeNodeSize - clientSet[clientInd].avalibleEdgeNode.size(),1);  //先分小的
                    //int bandwidthforEveryClient = Math.max(edgeNodeSet[j].bandwidth/(edgeNodeSet[j].avalibleClient.size()*100),10);   //均分,最佳结果：384281
                    //int bandwidthforEveryClient = 1000/clientSet[clientInd].avalibleEdgeNode.size();
                    //int bandwidthforEveryClient = Math.max(edgeNodeSet[j].bandwidth / ((edgeNodeSet[j].avalibleClient.size())*20) * (clientNodeSize - clientSet[Ind].avalibleEdgeNode.size()),1);  //将带宽分片
                    //int bandwidthforEveryClient = Math.max((clientNodeSize - clientSet[Ind].avalibleEdgeNode.size()),10);
                    int bandwidthforEveryClient = Math.max((int)(Math.pow(clientSet[clientInd].avalibleEdgeNode.size(),-0.4) * edgeNodeSet[j].bandwidth/40),1);    //参数为 0.75 与 40 时达到最好效果
                    if (demandDyn[clientInd][max5[t]] > bandwidthforEveryClient) {
                        int bandwidthget = Math.min(bandwidthforEveryClient, edgeNodeSet[j].bandwidthAvalible[max5[t]]);
                        demandDyn[clientInd][max5[t]] -= bandwidthget;
                        result[clientInd][j][max5[t]] += bandwidthget;
                        edgeNodeSet[j].bandwidthAvalible[max5[t]] -= bandwidthget;
                        if (edgeNodeSet[j].bandwidthAvalible[max5[t]] == 0) break;
                        Ind++;
                        continue;
                    } else {
                        if (demandDyn[clientInd][max5[t]] <= edgeNodeSet[j].bandwidthAvalible[max5[t]]) {
                            result[clientInd][j][max5[t]] += demandDyn[clientInd][max5[t]];
                            edgeNodeSet[j].bandwidthAvalible[max5[t]] -= demandDyn[clientInd][max5[t]];
                            demandDyn[clientInd][max5[t]] = 0;
                            Ind++;
                            continue;
                        } else {
                            result[clientInd][j][max5[t]] += edgeNodeSet[j].bandwidthAvalible[max5[t]];
                            demandDyn[clientInd][max5[t]] -= edgeNodeSet[j].bandwidthAvalible[max5[t]];
                            edgeNodeSet[j].bandwidthAvalible[max5[t]] = 0;
                            break;
                        }
                    }
                }
            }
        }




        int[] demandAllTNow = new int[T];
        for(int t1 = 0; t1 < T; t1++){
            int demandall = 0;
            for(int i = 0; i < clientNodeSize; i++){
                //   System.out.println(String.valueOf(t) + ":" + clientSet[i].name + ":" + demandDyn[i][t]);
                //    System.out.println("  ");
                demandall += demandDyn[i][t1];
            }
            demandAllTNow[t1] = demandall;
        }







        ArrayList<Integer> edgeSuperList = new ArrayList<Integer>();  //将一些边缘节点作为超级节点,超级节点尽量选择度大 带宽较小的节点

        for(int m = edgeEnd - 1; m >= 0; m--){  //初始化超级节点
            int j = edgelist2.get(m);
            edgeSuperList.add(j);
        }



        for(int m = 0; m < edgeSuperList.size(); m++){    //接下来对超级节点进行处理
            int j = edgeSuperList.get(m);
            System.out.println(edgeNodeSet[j].name);

            for(int x = 0; x < T/20; x++){  //迭代次数

/*                ArrayList<Integer> demandAllTListNow = sortT(demandAllTNow);   //对现在的总需求进行排序

                int demandAvalibleNow = 0;
                int t = demandAllTListNow.get(T-1);*/

                int t = maxT(demandAllTNow);
/*                int t = -1;
                //求当前超级节点在每一个时间的需求
                for(int t2 = T-1; t2 >= 0; t2--){
                    int t3 = demandAllTListNow.get(t2);
                    demandAvalibleNow = 0;
                    for(int i = 0; i < edgeNodeSet[j].avalibleClient.size(); i++){
                        demandAvalibleNow += demandDyn[edgeNodeSet[j].avalibleClient.get(i)][t3];
                    }
                    if(demandAvalibleNow > 0){
                        t = t3;
                        break;
                    }
                }   //找到当前需要超级节点的时刻
                if(t == -1) break; //发现所有时刻已经没需求了,换下一个超级节点*/

                int Ind = 0;
                while (true) {

                    //System.out.println("ss");
                    int duge = 0;
                    for (int y = 0; y < edgeNodeSet[j].avalibleClient.size(); y++) {
                        duge += demandDyn[edgeNodeSet[j].avalibleClient.get(y)][t];
                    }
                    if (duge == 0) break;
                    Ind = Ind % edgeNodeSet[j].avalibleClient.size();
                    int clientInd = edgeNodeSet[j].avalibleClient.get(Ind);
                    //int bandwidthforEveryClient = Math.max(edgeNodeSize - clientSet[clientInd].avalibleEdgeNode.size(),1);  //先分小的
                    //int bandwidthforEveryClient = Math.max(edgeNodeSet[j].bandwidth/(edgeNodeSet[j].avalibleClient.size()*100),10);   //均分,最佳结果：384281
                    //int bandwidthforEveryClient = 1000/clientSet[clientInd].avalibleEdgeNode.size();
                    //int bandwidthforEveryClient = Math.max(edgeNodeSet[j].bandwidth / ((edgeNodeSet[j].avalibleClient.size())*20) * (clientNodeSize - clientSet[Ind].avalibleEdgeNode.size()),1);  //将带宽分片
                    //int bandwidthforEveryClient = Math.max((clientNodeSize - clientSet[Ind].avalibleEdgeNode.size()),10);
                    int bandwidthforEveryClient = Math.max((int)(Math.pow(clientSet[clientInd].avalibleEdgeNode.size(),-0.8) * edgeNodeSet[j].bandwidth/40),1);    //参数为 0.75 与 40 时达到最好效果
                    if (demandDyn[clientInd][t] > bandwidthforEveryClient) {
                        int bandwidthget = Math.min(bandwidthforEveryClient, edgeNodeSet[j].bandwidthAvalible[t]);
                        demandDyn[clientInd][t] -= bandwidthget;
                        result[clientInd][j][t] += bandwidthget;
                        demandAllTNow[t] -= bandwidthget;
                        edgeNodeSet[j].bandwidthAvalible[t] -= bandwidthget;
                        if (edgeNodeSet[j].bandwidthAvalible[t] == 0) break;
                        Ind++;
                        continue;
                    } else {
                        if (demandDyn[clientInd][t] <= edgeNodeSet[j].bandwidthAvalible[t]) {
                            result[clientInd][j][t] += demandDyn[clientInd][t];
                            edgeNodeSet[j].bandwidthAvalible[t] -= demandDyn[clientInd][t];
                            demandAllTNow[t] -=demandDyn[clientInd][t];
                            demandDyn[clientInd][t] = 0;
                            Ind++;
                            continue;
                        } else {
                            result[clientInd][j][t] += edgeNodeSet[j].bandwidthAvalible[t];
                            demandDyn[clientInd][t] -= edgeNodeSet[j].bandwidthAvalible[t];
                            demandAllTNow[t] -= edgeNodeSet[j].bandwidthAvalible[t];
                            edgeNodeSet[j].bandwidthAvalible[t] = 0;
                            break;
                        }
                    }
                }

                edgeNodeSet[j].max5.add(t);

            }
        }

        //按照剩余客户总需求从小大到对时间排序
        int[] demandAllT = new int[T];
        for(int t = 0; t < T; t++){
            int demandall = 0;
            for(int i = 0; i < clientNodeSize; i++){
                //   System.out.println(String.valueOf(t) + ":" + clientSet[i].name + ":" + demandDyn[i][t]);
                //    System.out.println("  ");
                demandall += demandDyn[i][t];
            }
            demandAllT[t] = demandall;
        }
        ArrayList<Integer> demandAllTList = sortT(demandAllT);
        //    System.out.println(demandAllTList.size());
        for(int i = 0; i < demandAllTList.size(); i++){
            System.out.println(String.valueOf(demandAllTList.get(i)) + ":" + demandAllT[demandAllTList.get(i)]);
        }



        //后95%的平摊算法
/*
		for(int s = 0; s < 20; s++) {
			for (int m = 0; m < edgelist.size(); m++) {
				int j = edgelist.get(m);
				int[] demandComputeT = new int[T - edgeNodeSet[j].max5.size()];  //创造一个长度为0.95T长度的数组,存储仍需分配的时间
				int demandAllMin = -1;    //剩余95时刻最小非零需求
				//int demandAllMinT = -1;  //剩余95时刻最小非零需求的时刻
				int size0 = 0;
				for (int t = 0; t < T; t++) {
					if (edgeNodeSet[j].max5.contains(Integer.valueOf(t))) continue;   //跳过top5时刻
					//找到剩余95时刻最小非零需求
					int demandAllThisTime = 0;
					for (int k = 0; k < edgeNodeSet[j].avalibleClient.size(); k++) {
						demandAllThisTime += demandDyn[edgeNodeSet[j].avalibleClient.get(k)][t];
					}
					if (demandAllThisTime == 0) size0++;
					if ((demandAllThisTime <= demandAllMin && demandAllThisTime != 0) || demandAllMin == -1)
						demandAllMin = demandAllThisTime;
				}
				if(demandAllMin == 0) continue;
				if (size0 > s*T/20) continue;

				//System.out.println(demandAllMin);
				for (int t = 0; t < T; t++) {

					if (edgeNodeSet[j].max5.contains(Integer.valueOf(t))) continue;   //跳过top5时刻
					int demandAvarage = Math.min(demandAllMin, edgeNodeSet[j].bandwidth); //该时刻可以分配的数值
					int Ind = 0;
					int bandwidthforEveryClient = Math.max(demandAllMin / edgeNodeSet[j].avalibleClient.size(), 1);
					while (true) {

						int duge = 0;  //判断若用户需求均满足，跳出循环
						for (int y = 0; y < edgeNodeSet[j].avalibleClient.size(); y++) {
							duge += demandDyn[edgeNodeSet[j].avalibleClient.get(y)][t];
						}

						if (duge == 0) break;

						Ind = Ind % edgeNodeSet[j].avalibleClient.size();
						int clientInd = edgeNodeSet[j].avalibleClient.get(Ind);
						if (demandDyn[clientInd][t] > bandwidthforEveryClient) {

							int bandwidthget = Math.min(bandwidthforEveryClient, demandAvarage);
							demandDyn[clientInd][t] -= bandwidthget;
							result[clientInd][j][t] += bandwidthget;
							//多的变量,该时刻需要平均分配的带宽
							demandAvarage -= bandwidthget;
							edgeNodeSet[j].bandwidthAvalible[t] -= bandwidthget;
							if (demandAvarage == 0) break;
							Ind++;
							continue;
						} else {
							if (demandDyn[clientInd][t] <= demandAvarage) {
								result[clientInd][j][t] += demandDyn[clientInd][t];
								demandAvarage -= demandDyn[clientInd][t];
								edgeNodeSet[j].bandwidthAvalible[t] -= demandDyn[clientInd][t];
								demandDyn[clientInd][t] = 0;
								Ind++;
								continue;
							} else {
								result[clientInd][j][t] += demandAvarage;
								demandDyn[clientInd][t] -= demandAvarage;
								edgeNodeSet[j].bandwidthAvalible[t] -= demandAvarage;
								break;
							}
						}


					}
				}


			}
		}
*/
        //      System.out.println("//");
        for(int i = 0; i < clientNodeSize; i ++){
            if(demandDyn[i][90] != 0) {
                //       System.out.println(clientSet[i].name + ":" + demandDyn[i][90]);
                for (int j = 0; j < clientSet[i].avalibleEdgeNode.size(); j++) {
                    //    System.out.print(edgeNodeSet[clientSet[i].avalibleEdgeNode.get(j)].name + ":" + edgeNodeSet[clientSet[i].avalibleEdgeNode.get(j)].avalibleClient.size() + "  ");
                }
            }
        }
        //      System.out.println("//");










		/*
		for(int i = 0; i < edgeNodeSize; i++){
			System.out.println(edgeNodeSet[i].name + edgeNodeSet[i].avalibleClient);
		}
		System.out.println("-------");
		for(int i = 0; i < clientNodeSize; i++){
			System.out.println(clientSet[i].name + clientSet[i].avalibleEdgeNode);

		}
		 */

/*
        ArrayList<Integer> clientSort = sortClient();
        for(int i = 0; i < clientSort.size(); i++){
            int ind = clientSort.get(i);
            System.out.println(clientSet[ind].name +":"+ clientSet[ind].avalibleEdgeNode.size());
        }

        int tStart = demandAllTList.get(T-1);   //先分最大时刻确定初试价格
        for(int o = 0; o < clientNodeSize; o++){
            int i = clientSort.get(o);
            for(int j = 0; j < clientSet[i].avalibleEdgeNode.size(); j++){    //填满价格
                int edgeInd = clientSet[i].avalibleEdgeNode.get(j);
                int bandwidthUsedNow = edgeNodeSet[edgeInd].bandwidth - edgeNodeSet[edgeInd].bandwidthAvalible[tStart];
                if(edgeNodeSet[edgeInd].max5.contains(Integer.valueOf(tStart)) || edgeNodeSet[edgeInd].price95 <= bandwidthUsedNow)
                    continue;
                int bandwidthGive = edgeNodeSet[edgeInd].price95 - bandwidthUsedNow;
                if(demandDyn[i][tStart] <= bandwidthGive){
                    result[i][edgeInd][tStart] += demandDyn[i][tStart];
                    edgeNodeSet[edgeInd].bandwidthAvalible[tStart] -= demandDyn[i][tStart];
                    demandDyn[i][tStart] = 0;
                }else{
                    demandDyn[i][tStart] -= bandwidthGive;
                    result[i][edgeInd][tStart] += bandwidthGive;
                    edgeNodeSet[edgeInd].bandwidthAvalible[tStart] -= bandwidthGive;
                }
            }
            if(demandDyn[i][tStart] == 0) continue;

            HashSet<Integer> falg = new HashSet<Integer>();
            int avalibleEdgeSize = clientSet[i].avalibleEdgeNode.size();
            int bandwidthNeed = demandDyn[i][tStart];
            int j = 0;
            while(true){
                if(falg.size()==clientSet[i].avalibleEdgeNode.size()) return;
                j = j % avalibleEdgeSize;
                //int bandwidthforEveryClient = edgeNodeSet[clientSet[i].avalibleEdgeNode.get(j)].bandwidth/edgeNodeSet[clientSet[i].avalibleEdgeNode.get(j)].avalibleClient.size()/50;
                //int bandwidthforEveryClient = Math.max(edgeNodeSet[clientSet[i].avalibleEdgeNode.get(j)].bandwidth/edgeNodeSet[clientSet[i].avalibleEdgeNode.get(j)].avalibleClient.size()/500, 1);
                //int bandwidthforEveryClient =Math.max( bandwidthforEveryClientAverage[j], 1);
                //int bandwidthforEveryClient = (edgeNodeSet[clientSet[i].avalibleEdgeNode.get(j)].avalibleClient.size() )*(edgeNodeSet[clientSet[i].avalibleEdgeNode.get(j)].avalibleClient.size() );  //将度作为切片大小，度越大分得越多
                //int bandwidthforEveryClient = 1;
                int bandwidthforEveryClient = (int)(Math.pow(edgeNodeSet[clientSet[i].avalibleEdgeNode.get(j)].avalibleClient.size(),1.6));
                int bandwidthAvalibelNow = edgeNodeSet[clientSet[i].avalibleEdgeNode.get(j)].bandwidthAvalible[tStart];
                if(bandwidthNeed > bandwidthforEveryClient){
                    int bandwidthgive = Math.min(bandwidthforEveryClient, bandwidthAvalibelNow);
                    result[i][clientSet[i].avalibleEdgeNode.get(j)][tStart] += bandwidthgive;
                    if(bandwidthgive == bandwidthNeed) falg.add(j);
                    edgeNodeSet[clientSet[i].avalibleEdgeNode.get(j)].bandwidthAvalible[tStart] -= bandwidthgive;
                    demandDyn[i][tStart] -= bandwidthgive;
                    bandwidthNeed -= bandwidthgive;
                    j++;
                    continue;
                }else{
                    if(bandwidthNeed <= bandwidthAvalibelNow){
                        result[i][clientSet[i].avalibleEdgeNode.get(j)][tStart] += bandwidthNeed;
                        edgeNodeSet[clientSet[i].avalibleEdgeNode.get(j)].bandwidthAvalible[tStart] -= bandwidthNeed;
                        demandDyn[i][tStart] -= bandwidthNeed;
                        break;
                    }else{
                        bandwidthNeed -= bandwidthAvalibelNow;
                        result[i][clientSet[i].avalibleEdgeNode.get(j)][tStart] += bandwidthAvalibelNow;
                        demandDyn[i][tStart] -= bandwidthAvalibelNow;
                        edgeNodeSet[clientSet[i].avalibleEdgeNode.get(j)].bandwidthAvalible[tStart] = 0;
                        falg.add(j);
                        j++;

                        continue;
                    }
                }
            }

            for(int n = 0; n < clientSet[i].avalibleEdgeNode.size(); n++){   //更新price95的值
                int edgeInd = clientSet[i].avalibleEdgeNode.get(n);
                int bandwidthUsedNow = edgeNodeSet[edgeInd].bandwidth - edgeNodeSet[edgeInd].bandwidthAvalible[tStart];
                if(edgeNodeSet[edgeInd].max5.contains(tStart) || bandwidthUsedNow <= edgeNodeSet[edgeInd].price95) continue;
                edgeNodeSet[edgeInd].price95 = bandwidthUsedNow;
            }

        }
*/

 /*       while(true){
            int t =demandAllTList.get(T-1);
            int i = 0;
            int demandSum = 0;
            int demandforEveryEdge = 0;
            for(int r = 0; r < clientNodeSize; r++){
                for(int u1 = T - 1; u1 >= 0; u1--){
                    int u = demandAllTList.get(u1);
                    demandSum += demandDyn[r][u];
                    if(demandDyn[i][t] == 0 ||(clientSet[r].avalibleEdgeNode.size() < clientSet[i].avalibleEdgeNode.size() && demandDyn[r][u] > 0)){
                        t = u;
                        i = r;
                    }
                }
            }
            if(demandSum == 0) break;
            for(int j = 0; j < clientSet[i].avalibleEdgeNode.size(); j++){    //填满价格
                int edgeInd = clientSet[i].avalibleEdgeNode.get(j);
                int bandwidthUsedNow = edgeNodeSet[edgeInd].bandwidth - edgeNodeSet[edgeInd].bandwidthAvalible[t];
                if(edgeNodeSet[edgeInd].max5.contains(Integer.valueOf(t)) || edgeNodeSet[edgeInd].price95 <= bandwidthUsedNow)
                    continue;
                int bandwidthGive = edgeNodeSet[edgeInd].price95 - bandwidthUsedNow;
                if(demandDyn[i][t] <= bandwidthGive){
                    result[i][edgeInd][t] += demandDyn[i][t];
                    edgeNodeSet[edgeInd].bandwidthAvalible[t] -= demandDyn[i][t];
                    demandDyn[i][t] = 0;
                }else{
                    demandDyn[i][t] -= bandwidthGive;
                    result[i][edgeInd][t] += bandwidthGive;
                    edgeNodeSet[edgeInd].bandwidthAvalible[t] -= bandwidthGive;
                }
            }
            if(demandDyn[i][t] == 0) continue;
            HashSet<Integer> falg = new HashSet<Integer>();
            int avalibleEdgeSize = clientSet[i].avalibleEdgeNode.size();
            int bandwidthNeed = demandDyn[i][t];
            int j = 0;
            while(true){
                if(falg.size()==clientSet[i].avalibleEdgeNode.size()) return;
                j = j % avalibleEdgeSize;
                //int bandwidthforEveryClient = edgeNodeSet[clientSet[i].avalibleEdgeNode.get(j)].bandwidth/edgeNodeSet[clientSet[i].avalibleEdgeNode.get(j)].avalibleClient.size()/50;
                //int bandwidthforEveryClient = Math.max(edgeNodeSet[clientSet[i].avalibleEdgeNode.get(j)].bandwidth/edgeNodeSet[clientSet[i].avalibleEdgeNode.get(j)].avalibleClient.size()/500, 1);
                //int bandwidthforEveryClient =Math.max( bandwidthforEveryClientAverage[j], 1);
                //int bandwidthforEveryClient = (edgeNodeSet[clientSet[i].avalibleEdgeNode.get(j)].avalibleClient.size() )*(edgeNodeSet[clientSet[i].avalibleEdgeNode.get(j)].avalibleClient.size() );  //将度作为切片大小，度越大分得越多
                //int bandwidthforEveryClient = 1;
                int bandwidthforEveryClient = (int)(Math.pow(edgeNodeSet[clientSet[i].avalibleEdgeNode.get(j)].avalibleClient.size(),1.6));
                int bandwidthAvalibelNow = edgeNodeSet[clientSet[i].avalibleEdgeNode.get(j)].bandwidthAvalible[t];
                if(bandwidthNeed > bandwidthforEveryClient){
                    int bandwidthgive = Math.min(bandwidthforEveryClient, bandwidthAvalibelNow);
                    result[i][clientSet[i].avalibleEdgeNode.get(j)][t] += bandwidthgive;
                    if(bandwidthgive == bandwidthNeed) falg.add(j);
                    edgeNodeSet[clientSet[i].avalibleEdgeNode.get(j)].bandwidthAvalible[t] -= bandwidthgive;
                    demandDyn[i][t] -= bandwidthgive;
                    bandwidthNeed -= bandwidthgive;
                    j++;
                    continue;
                }else{
                    if(bandwidthNeed <= bandwidthAvalibelNow){
                        result[i][clientSet[i].avalibleEdgeNode.get(j)][t] += bandwidthNeed;
                        edgeNodeSet[clientSet[i].avalibleEdgeNode.get(j)].bandwidthAvalible[t] -= bandwidthNeed;
                        demandDyn[i][t] -= bandwidthNeed;
                        break;
                    }else{
                        bandwidthNeed -= bandwidthAvalibelNow;
                        result[i][clientSet[i].avalibleEdgeNode.get(j)][t] += bandwidthAvalibelNow;
                        demandDyn[i][t] -= bandwidthAvalibelNow;
                        edgeNodeSet[clientSet[i].avalibleEdgeNode.get(j)].bandwidthAvalible[t] = 0;
                        falg.add(j);
                        j++;

                        continue;
                    }
                }
            }

            for(int n = 0; n < clientSet[i].avalibleEdgeNode.size(); n++){   //更新price95的值
                int edgeInd = clientSet[i].avalibleEdgeNode.get(n);
                int bandwidthUsedNow = edgeNodeSet[edgeInd].bandwidth - edgeNodeSet[edgeInd].bandwidthAvalible[t];
                if(edgeNodeSet[edgeInd].max5.contains(t) || bandwidthUsedNow <= edgeNodeSet[edgeInd].price95) continue;
                edgeNodeSet[edgeInd].price95 = bandwidthUsedNow;
            }
        }*/



        ArrayList<Integer> clientSort = sortClient();
        for(int i = 0; i < clientSort.size(); i++){
            int ind = clientSort.get(i);
            //   System.out.println(clientSet[ind].name +":"+ clientSet[ind].avalibleEdgeNode.size());
        }


        for(int m = 0; m <demandAllTList.size(); m++){  //平分

            int t = demandAllTList.get(m);   //按总需求量由大到小的时间接入
            //int t = m;                        //顺序时间接入
            clientSort = sortClient2(t);
            for(int o = 0; o < clientSort.size(); o++){

                int i = clientSort.get(o);
                int j = 0;
                int avalibleEdgeSize = clientSet[i].avalibleEdgeNode.size();
                int bandwidthNeed = demandDyn[i][t];

                int avaliblePriceall = 0;
                for(int x = 0; x < clientSet[i].avalibleEdgeNode.size(); x++){    //填满价格
                    int edgeInd = clientSet[i].avalibleEdgeNode.get(x);
                    int avalibleThisEdge = -edgeNodeSet[edgeInd].bandwidth + edgeNodeSet[edgeInd].price95 + edgeNodeSet[edgeInd].bandwidthAvalible[t];
                    if(avalibleThisEdge > 0) avaliblePriceall += avalibleThisEdge;
                }
                if (bandwidthNeed >= avaliblePriceall){
                    demandDyn[i][t] -= avaliblePriceall;
                    for(int x = 0; x < clientSet[i].avalibleEdgeNode.size(); x++){    //填满价格
                        int edgeInd = clientSet[i].avalibleEdgeNode.get(x);
                        int avalibleThisEdge = -edgeNodeSet[edgeInd].bandwidth + edgeNodeSet[edgeInd].price95 + edgeNodeSet[edgeInd].bandwidthAvalible[t];
                        if(avalibleThisEdge > 0){
                            result[i][edgeInd][t] += avalibleThisEdge;
                            edgeNodeSet[edgeInd].bandwidthAvalible[t] -= avalibleThisEdge;
                        }
                    }
                }else
                {
                    HashSet<Integer> falg0 = new HashSet<Integer>();
                    // System.out.println("ss");
                    j = -1;
                    while (true) {

                        if (falg0.size() == clientSet[i].avalibleEdgeNode.size()) break;
                        if(j < 0)
                            j = (j + clientSet[i].avalibleEdgeNode.size()) % clientSet[i].avalibleEdgeNode.size();
                        //  System.out.println(j);
                        int edgeInd = clientSet[i].avalibleEdgeNode.get(j);
                        int bandwidthUsedNow = edgeNodeSet[edgeInd].bandwidth - edgeNodeSet[edgeInd].bandwidthAvalible[t];

                        //int bandwidthforEveryClient = edgeNodeSet[clientSet[i].avalibleEdgeNode.get(j)].bandwidth/edgeNodeSet[clientSet[i].avalibleEdgeNode.get(j)].avalibleClient.size()/50;
                        //int bandwidthforEveryClient = Math.max(edgeNodeSet[clientSet[i].avalibleEdgeNode.get(j)].bandwidth/edgeNodeSet[clientSet[i].avalibleEdgeNode.get(j)].avalibleClient.size()/500, 1);
                        //int bandwidthforEveryClient =Math.max( bandwidthforEveryClientAverage[j], 1);
                        //int bandwidthforEveryClient = (edgeNodeSet[clientSet[i].avalibleEdgeNode.get(j)].avalibleClient.size() )*(edgeNodeSet[clientSet[i].avalibleEdgeNode.get(j)].avalibleClient.size() );  //将度作为切片大小，度越大分得越多
                        //int bandwidthforEveryClient = 1;
                        int bandwidthforEveryClient = Math.max((int) (Math.pow(edgeNodeSet[edgeInd].avalibleClient.size(), -1) * edgeNodeSize * 15),1);
                        //int bandwidthforEveryClient = 100;

                        int bandwidthAvalibelNow = edgeNodeSet[edgeInd].price95 - bandwidthUsedNow;
                        if (bandwidthAvalibelNow <= 0) {
                            falg0.add(j);
                            j--;
                            continue;
                        }
                        // System.out.println(bandwidthAvalibelNow);
                        if (bandwidthNeed > bandwidthforEveryClient) {   //一波分不完
                            int bandwidthgive = Math.min(bandwidthforEveryClient, bandwidthAvalibelNow);
                            result[i][clientSet[i].avalibleEdgeNode.get(j)][t] += bandwidthgive;
                            if (bandwidthgive == bandwidthNeed) falg0.add(j);
                            edgeNodeSet[clientSet[i].avalibleEdgeNode.get(j)].bandwidthAvalible[t] -= bandwidthgive;
                            demandDyn[i][t] -= bandwidthgive;
                            bandwidthNeed -= bandwidthgive;
                            j--;
                            continue;
                        } else {
                            if (bandwidthNeed <= bandwidthAvalibelNow) {
                                result[i][clientSet[i].avalibleEdgeNode.get(j)][t] += bandwidthNeed;
                                edgeNodeSet[clientSet[i].avalibleEdgeNode.get(j)].bandwidthAvalible[t] -= bandwidthNeed;
                                demandDyn[i][t] -= bandwidthNeed;
                                break;
                            } else {
                                bandwidthNeed -= bandwidthAvalibelNow;
                                result[i][clientSet[i].avalibleEdgeNode.get(j)][t] += bandwidthAvalibelNow;
                                demandDyn[i][t] -= bandwidthAvalibelNow;
                                edgeNodeSet[clientSet[i].avalibleEdgeNode.get(j)].bandwidthAvalible[t] -= bandwidthAvalibelNow;
                                falg0.add(j);
                                j--;
                                continue;
                            }
                        }

                    }
                }

/*                for(int j = 0; j < clientSet[i].avalibleEdgeNode.size(); j++){    //填满价格
                    int edgeInd = clientSet[i].avalibleEdgeNode.get(j);
                    int bandwidthUsedNow = edgeNodeSet[edgeInd].bandwidth - edgeNodeSet[edgeInd].bandwidthAvalible[t];
                    if(edgeNodeSet[edgeInd].max5.contains(Integer.valueOf(t)) || edgeNodeSet[edgeInd].price95 <= bandwidthUsedNow)
                        continue;
                    int bandwidthGive = edgeNodeSet[edgeInd].price95 - bandwidthUsedNow;
                    if(demandDyn[i][t] <= bandwidthGive){
                        result[i][edgeInd][t] += demandDyn[i][t];
                        edgeNodeSet[edgeInd].bandwidthAvalible[t] -= demandDyn[i][t];
                        demandDyn[i][t] = 0;
                    }else{
                        demandDyn[i][t] -= bandwidthGive;
                        result[i][edgeInd][t] += bandwidthGive;
                        edgeNodeSet[edgeInd].bandwidthAvalible[t] -= bandwidthGive;
                    }
                }*/
                HashSet<Integer> falg = new HashSet<Integer>();

                bandwidthNeed = demandDyn[i][t];
                j = 0;
                while(true){

                    if(falg.size()==clientSet[i].avalibleEdgeNode.size()) return;
                    j = j % avalibleEdgeSize;
                    //int bandwidthforEveryClient = edgeNodeSet[clientSet[i].avalibleEdgeNode.get(j)].bandwidth/edgeNodeSet[clientSet[i].avalibleEdgeNode.get(j)].avalibleClient.size()/50;
                    //int bandwidthforEveryClient = Math.max(edgeNodeSet[clientSet[i].avalibleEdgeNode.get(j)].bandwidth/edgeNodeSet[clientSet[i].avalibleEdgeNode.get(j)].avalibleClient.size()/500, 1);
                    //int bandwidthforEveryClient =Math.max( bandwidthforEveryClientAverage[j], 1);
                    //int bandwidthforEveryClient = (edgeNodeSet[clientSet[i].avalibleEdgeNode.get(j)].avalibleClient.size() )*(edgeNodeSet[clientSet[i].avalibleEdgeNode.get(j)].avalibleClient.size() );  //将度作为切片大小，度越大分得越多
                    //int bandwidthforEveryClient = 1;
                    int bandwidthforEveryClient = (int)(Math.pow(edgeNodeSet[clientSet[i].avalibleEdgeNode.get(j)].avalibleClient.size(),1.4));
                    int bandwidthAvalibelNow = edgeNodeSet[clientSet[i].avalibleEdgeNode.get(j)].bandwidthAvalible[t];
                    if(bandwidthNeed > bandwidthforEveryClient){
                        int bandwidthgive = Math.min(bandwidthforEveryClient, bandwidthAvalibelNow);
                        result[i][clientSet[i].avalibleEdgeNode.get(j)][t] += bandwidthgive;
                        if(bandwidthgive == bandwidthNeed) falg.add(j);
                        edgeNodeSet[clientSet[i].avalibleEdgeNode.get(j)].bandwidthAvalible[t] -= bandwidthgive;
                        demandDyn[i][t] -= bandwidthgive;
                        bandwidthNeed -= bandwidthgive;
                        j++;
                        continue;
                    }else{
                        if(bandwidthNeed <= bandwidthAvalibelNow){
                            result[i][clientSet[i].avalibleEdgeNode.get(j)][t] += bandwidthNeed;
                            edgeNodeSet[clientSet[i].avalibleEdgeNode.get(j)].bandwidthAvalible[t] -= bandwidthNeed;
                            demandDyn[i][t] -= bandwidthNeed;
                            break;
                        }else{
                            bandwidthNeed -= bandwidthAvalibelNow;
                            result[i][clientSet[i].avalibleEdgeNode.get(j)][t] += bandwidthAvalibelNow;
                            demandDyn[i][t] -= bandwidthAvalibelNow;
                            edgeNodeSet[clientSet[i].avalibleEdgeNode.get(j)].bandwidthAvalible[t] = 0;
                            falg.add(j);
                            j++;

                            continue;
                        }
                    }
                }

                for(int n = 0; n < clientSet[i].avalibleEdgeNode.size(); n++){   //更新price95的值
                    int edgeInd = clientSet[i].avalibleEdgeNode.get(n);
                    int bandwidthUsedNow = edgeNodeSet[edgeInd].bandwidth - edgeNodeSet[edgeInd].bandwidthAvalible[t];
                    if(edgeNodeSet[edgeInd].max5.contains(t) || bandwidthUsedNow <= edgeNodeSet[edgeInd].price95) continue;
                    edgeNodeSet[edgeInd].price95 = bandwidthUsedNow;
                }
            }



        }

        int test = 0;
        for(int i = 0; i < clientNodeSize; i++){
            for(int t = 0; t < T; t++){
                test += demandDyn[i][t];
            }
        }

        System.out.println(test);

/*		for(int i=0; i < clientNodeSize; i++){
			System.out.println(clientSet[i].avalibleEdgeNode);
		}*/

        output(result);  //输出文件

    }

    public static int getqos(){
        String filepath = "/data/config.ini";
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(filepath));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        String line1 = null;
        int qos = 0;
        try {
            line1 = br.readLine();
            line1 = br.readLine();
            String[] x = line1.split("=");
            qos = Integer.parseInt(x[1]);
            //System.out.println(qos);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return qos;
    }


    public static void output(int[][][] result){
        String path = "/output/solution.txt";
        BufferedWriter  bw = null;
        try{
            bw = new BufferedWriter(new FileWriter(path));
            for(int t = 0; t < T; t++){
                for(int i = 0; i < clientNodeSize; i++){
                    bw.write(clientSet[i].name + ":");
                    int flag = 0;
                    for(int j = 0; j < edgeNodeSize; j++){
                        if(result[i][j][t] == 0) continue;
                        else{
                            if(flag != 0) bw.write(",");
                            bw.write("<" + edgeNodeSet[j].name + "," + String.valueOf(result[i][j][t]) + ">");
                            flag = 1;
                        }
                    }
                    if(i == clientNodeSize-1 && t == T-1) break;
                    bw.newLine();
                }
            }
            bw.close();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<Integer> sortClient(){
        ArrayList<Integer> result = new ArrayList<Integer>();
        HashSet<Integer> sethash = new HashSet<Integer>();

        for(int i = 0; i < clientNodeSize; i++){
            sethash.add(clientSet[i].avalibleEdgeNode.size());
        }

        ArrayList<Integer> sortlist = new ArrayList<Integer>();
        for(Integer x : sethash){
            sortlist.add(x);
        }
        Collections.sort(sortlist);
        for(int i = 0; i < sortlist.size(); i++){
            for(int j = 0; j < clientSet.length; j++){
                if(clientSet[j].avalibleEdgeNode.size() == sortlist.get(i)){
                    result.add(j);
                }
            }
        }
        //System.out.println(result);
        return result;

    }

    public static ArrayList<Integer> sortClient2(int t){
        ArrayList<Integer> result = new ArrayList<Integer>();
        HashSet<Integer> sethash = new HashSet<Integer>();
        int[] size = new int[clientNodeSize];



        for(int i = 0; i < clientNodeSize; i++){
            int avalibleEdgeNodeDetail = 0;
            for(int j = 0; j < clientSet[i].avalibleEdgeNode.size(); j++){
                int edgeInd = clientSet[i].avalibleEdgeNode.get(j);
                if(edgeNodeSet[edgeInd].max5.contains(t)){
                    continue;
                }
                avalibleEdgeNodeDetail++;
            }
            size[i] = avalibleEdgeNodeDetail;
            sethash.add(avalibleEdgeNodeDetail);
        }

        ArrayList<Integer> sortlist = new ArrayList<Integer>();
        for(Integer x : sethash){
            sortlist.add(x);
        }
        Collections.sort(sortlist);
        for(int i = 0; i < sortlist.size(); i++){
            for(int j = 0; j < size.length; j++){
                if(size[j] == sortlist.get(i)){
                    result.add(j);
                }
            }
        }
        //System.out.println(result);
        return result;

    }

/*	public static int[] sortList(int[] x, int bandwidth){   //取出数组中最大的前百分之五的索引
		int[] result = new int[T/20];
		ArrayList<Integer> timeAvalible = new ArrayList<Integer>();
		for(int i = 0; i < x.length; i++){
			if(x[i] >= bandwidth) timeAvalible.add(i);
			x[i] = 0;
		}
		int[] demandAllT = new int[T];
		for(int t = 0; t < T; t++){
			int demandall = 0;
			for(int i = 0; i < clientNodeSize; i++){
				demandall += demandDyn[i][t];
			}
			demandAllT[t] = demandall;
		}
		ArrayList<Integer> demandAllTList = sortT(demandAllT);

		int k = 0;
		for(int i = T-1; i >= 0; i--){
			if(timeAvalible.contains(demandAllTList.get(i))){
				result[k] = demandAllTList.get(i);
				k++;
			}
			if(k >= result.length) return result;
		}
		for(int i = k; i < result.length; i++){
			int temp = 0;
			for(int j = 0; j < x.length; j++){
				if(x[j] > temp){
					temp = x[j];
					result[i] = j;
				}
			}
			x[result[i]] = 0;
		}
		return result;
	}*/

    public static int[] sortList(int[] x, int bandwidth){   //取出数组中最大的前百分之五的索引
        int[] result = new int[T/20];

        for(int i = 0; i < result.length; i++){
            int temp = 0;
            for(int j = 0; j < x.length; j++){
                if(x[j] > temp){
                    temp = x[j];
                    result[i] = j;
                }
            }
            x[result[i]] = 0;
        }
        return result;
    }

    public static int getmaxT(int j, int[] demandCompute){
        int result = -1;
        for(int i = 0; i < T; i++){
            if(edgeNodeSet[j].max5.contains(i)) continue;
            if(result == -1 || demandCompute[result] < demandCompute[i]) result = i;
        }
        return result;
    }

    public static int bandwidthNeedMax(int j , ArrayList<Integer> timeList, int t){
        int result = 0;
        for(int i = t + 1 ; i < timeList.size(); i++){
            int timeInd = timeList.get(t);
            int bandwidthThisTime = 0;
            for(int m = 0; m < edgeNodeSet[j].avalibleClient.size(); m++){
                bandwidthThisTime += demandDyn[edgeNodeSet[j].avalibleClient.get(m)][timeInd];
            }
            if(bandwidthThisTime > result) result = bandwidthThisTime;
        }
        return result;
    }

    public static ArrayList<Integer> sortEdge(EdgeNode[] set){
        ArrayList<Integer> result = new ArrayList<Integer>();
        HashSet<Integer> sethash = new HashSet<Integer>();

        for(int i = 0; i < set.length; i++){
            sethash.add(edgeNodeSet[i].avalibleClient.size());
        }


        ArrayList<Integer> sortlist = new ArrayList<Integer>();
        for(Integer x : sethash){
            sortlist.add(x);
        }
        Collections.sort(sortlist);
        for(int i = 0; i < sortlist.size(); i++){
            if(sortlist.get(i) == 0) continue;
            for(int j = 0; j < set.length; j++){
                if(set[j].avalibleClient.size() == sortlist.get(i)){
                    result.add(j);
                }
            }
        }
        //System.out.println(result);
        return result;
    }
    public static ArrayList<Integer> sortEdge2(EdgeNode[] set){

        ArrayList<Integer> result = new ArrayList<Integer>();
        HashSet<Integer> sethash = new HashSet<Integer>();

        for(int i = 0; i < set.length; i++){
            if(edgeNodeSet[i].avalibleClient.size() == 0) continue;
            sethash.add(edgeNodeSet[i].bandwidth/edgeNodeSet[i].avalibleClient.size());
        }


        ArrayList<Integer> sortlist = new ArrayList<Integer>();
        sortlist.addAll(sethash);
        Collections.sort(sortlist);
        for(int i = 0; i < sortlist.size(); i++){
            if(sortlist.get(i) == 0) continue;
            for(int j = 0; j < set.length; j++){
                if(edgeNodeSet[j].avalibleClient.size() == 0) continue;
                if(edgeNodeSet[j].bandwidth/edgeNodeSet[j].avalibleClient.size() == sortlist.get(i)){

                    result.add(j);
                }
            }
        }
        //System.out.println(result);
        return result;
    }

    public static ArrayList<Integer> sortEdge3(EdgeNode[] set){

        ArrayList<Integer> result = new ArrayList<Integer>();
        HashSet<Integer> sethash = new HashSet<Integer>();

        for(int i = 0; i < set.length; i++){
            if(edgeNodeSet[i].avalibleClient.size() == 0) continue;
            sethash.add(edgeNodeSet[i].bandwidth*edgeNodeSet[i].avalibleClient.size());
        }


        ArrayList<Integer> sortlist = new ArrayList<Integer>();
        sortlist.addAll(sethash);
        Collections.sort(sortlist);
        for(int i = 0; i < sortlist.size(); i++){
            if(sortlist.get(i) == 0) continue;
            for(int j = 0; j < set.length; j++){
                if(edgeNodeSet[j].avalibleClient.size() == 0) continue;
                if(edgeNodeSet[j].bandwidth*edgeNodeSet[j].avalibleClient.size() == sortlist.get(i)){

                    result.add(j);
                }
            }
        }
        //System.out.println(result);
        return result;
    }

    public static ArrayList<Integer> sortT(int[] demand){
        ArrayList<Integer>  result = new ArrayList<Integer>();
        HashSet<Integer> sethash = new HashSet<Integer>();
        ArrayList<Integer> sortlist = new ArrayList<Integer>();
        for(int i = 0; i < T; i++){
            sethash.add(demand[i]);
        }
        sortlist.addAll(sethash);
        Collections.sort(sortlist);
        for(int i = 0; i < sortlist.size(); i++){
            for(int j = 0; j < T; j++){
                if(sortlist.get(i) == demand[j])
                    result.add(j);
            }
        }
        return result;
    }

    public static int maxT(int[] x){
        int result = -1;
        for(int i = 0; i < x.length; i++){
            if(result == -1 || x[i] > x[result] )
                result = i;
        }
        return result;
    }

    public static ArrayList<List<String>>  getdata(String filepath){
        int i =0;
        BufferedReader br = null;

        try {
            br = new BufferedReader(new FileReader(filepath));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        String line1 = null;
        ArrayList demand = new ArrayList<List<String>>();
        try {
            //line1 = br.readLine();

            while((line1 = br.readLine()) != null){
//                System.out.println(line1);
                String[] x = line1.split(",");  //已经拿到数组了
//                System.out.println(x[0]);
                List<String> x1 = Arrays.asList(x);  //又把数组转为列表了
//                System.out.println(x1);
//                System.out.println(i++);
                //ArrayList<String> x2 = new ArrayList<String>(x1);  //又把list转为ArrayList
                //x2.remove(0);
                demand.add(x1);
//                System.out.println(x1.get(0));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return demand;
    }
 /*   public static ArrayList<List<String>>  getdata(String filepath){

        BufferedReader br = null;

        try {
            br = new BufferedReader(new FileReader(filepath));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        String line1 = null;
        ArrayList demand = new ArrayList<List<String>>();
        try {
            //line1 = br.readLine();
            while((line1 = br.readLine()) != null){
                String[] x = line1.split(",");  //已经拿到数组了

                List<String> x1 = Arrays.asList(x);  //又把数组转为列表了
                //ArrayList<String> x2 = new ArrayList<String>(x1);  //又把list转为ArrayList
                //x2.remove(0);
                demand.add(x1);
                //System.out.println(x1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return demand;
    }*/
}

class ClientNode{
    public String name;          //节点名字
    public int[] demand;         //节点带宽需求,按时间排列
    public ArrayList<Integer> avalibleEdgeNode; //该可用边缘节点
    public ClientNode(String name, int[] demand, ArrayList<Integer> avalibleEdgeNode){
        this.name = name;
        this.demand = demand;
        this.avalibleEdgeNode = avalibleEdgeNode;
    }
}

class EdgeNode{
    public String name; //边缘节点名字
    public int bandwidth; //边缘节点带宽
    public int bandwidthAvalible[];
    public ArrayList<Integer> avalibleClient;
    public ArrayList<Integer> max5 = new ArrayList<Integer>();
    public int price95 = 0;
    //	public int bandwidthforEveryClient;
    public EdgeNode(String name, int bandwidth, ArrayList<Integer> avalibleClient){
        this.name = name;
        this.bandwidth = bandwidth;
        //this.bandwidthAvalible = bandwidth;
        this.avalibleClient = avalibleClient;
//		this.bandwidthAvalible = bandwidth/avalibleClient.size();
    }
    public EdgeNode(String name, int bandwidth){
        this.name = name;
        this.bandwidth = bandwidth;
        //	this.bandwidthAvalible = bandwidth;

    }
}
