import os
import sys
import math
import time
from io import StringIO
from subprocess import getoutput
from typing import Tuple, List
from abc import ABC, abstractmethod
import numpy as np

# import mpld3
# import matplotlib.pyplot as plt
# from mpld3._server import serve as mpld3_server


cname, sname, qos, qos_lim = None, None, None, None
stream_id_map, stream_id_list = None, None
base_cost = 0
center_cost = 0.0
client_demand = None
bandwidth = None
time_label = None
cname_map = {}
sname_map = {}


class IOFile():
    demand = r'\data\demand.csv'
    qos = r'\data\qos.csv'
    bandwidth = r'\data\site_bandwidth.csv'
    config = r'D:\data\config.ini'
    output = r'\output\solution.txt'



def err_print(msg, original_line=None):
    print('ERROR  ' * 10)
    print(msg)
    if original_line:
        print(original_line)
    print('ERROR  ' * 10)
    exit(1)


def out_print(msg):
    print('RESULT  ' * 10)
    print(msg)
    print('RESULT  ' * 10)


def read_demand() -> Tuple[List[str], List[int]]:
    fname = IOFile.demand
    with open(fname) as f:
        data = f.read().splitlines()
    client_name = data[0].split(',')[2:]
    client_demand = []
    time_label = []
    prev_time_label = 'the placeholder of previous time'
    record_in_this_time = {}
    stream_id_map = {}
    stream_id_list = []
    stream_id_idx_cnt = 0
    for each in data[1:]:
        d = each.split(',')
        current_time_label = d[0]
        current_stream_id = d[1]
        if current_stream_id not in stream_id_map:
            idx = stream_id_idx_cnt
            stream_id_map[current_stream_id] = idx
            stream_id_list.append(current_stream_id)
            stream_id_idx_cnt += 1
        else:
            idx = stream_id_map[current_stream_id]
        if prev_time_label != current_time_label:
            prev_time_label = current_time_label
            time_label.append(current_time_label)
            if record_in_this_time:
                client_demand.append(record_in_this_time)
            record_in_this_time = {}
        current_demand = list(map(int, d[2:]))
        record_in_this_time[idx] = current_demand
    client_demand.append(record_in_this_time)

    stream_id_count=0
    for keys in stream_id_map:
        stream_id_count+=1

    return time_label, client_name, client_demand, stream_id_map, stream_id_list,stream_id_count


def read_server_bandwidth() -> Tuple[List[str], List[int]]:
    fname = IOFile.bandwidth
    with open(fname) as f:
        data = f.read().splitlines()
    server_name = []
    server_bandwidth = []
    for each in data[1:]:
        a, b = each.split(',')
        server_name.append(a)
        server_bandwidth.append(int(b))
    return server_name, server_bandwidth


def read_qos() -> Tuple[List[str], List[str], List[List[int]]]:
    fname = IOFile.qos
    with open(fname) as f:
        data = f.read().splitlines()
    client_name = data[0].split(',')[1:]
    server_name = []
    qos_array = []
    for each in data[1:]:
        d = each.split(',')
        server_name.append(d[0])
        qos_array.append(list(map(int, d[1:])))
    return client_name, server_name, qos_array


def read_qos_limit() ->Tuple[int,int,float] :
    global qos_lim, base_cost, center_cost
    fname = IOFile.config
    with open(fname) as f:
        data = f.read().splitlines()
    qos_lim = int(data[1].split('=')[1])
    base_cost = int(data[2].split('=')[1])
    center_cost = float(data[3].split('=')[1])
    return qos_lim, base_cost ,center_cost

def validate_file_exist():
    if not os.path.exists(IOFile.output):
        if os.path.exists('/' + IOFile.output):
            IOFile.output = '/' + IOFile.output
        else:
            err_print('can not find solution.txt in ./output/ or /output/')
    if not os.path.exists(IOFile.demand):
        if os.path.exists('/' + IOFile.demand):
            IOFile.demand = '/' + IOFile.demand
            IOFile.qos = '/' + IOFile.qos
            IOFile.bandwidth = '/' + IOFile.bandwidth
            IOFile.config = '/' + IOFile.config
        else:
            err_print('can not find input file in ./data/ or /data/')


def get_input_data():
    global cname, sname, qos, qos_lim, bandwidth, client_demand, time_label,stream_kind_count
    global stream_id_map, stream_id_list, base_cost,center_cost
    cname, sname, qos = read_qos()
    for idx, name in enumerate(sname):
        sname_map[name] = idx
    time_label, client_name, client_demand, stream_id_map, stream_id_list,stream_kind_count = read_demand()
    # print(stream_kind_count)
    for idx, name in enumerate(client_name):
        cname_map[name] = idx
    client_idx_list = []
    for c in client_name:
        idx = cname.index(c)
        client_idx_list.append(idx)
    qos = np.array(qos)[:, client_idx_list]
    cname = client_name
    # client_demand = np.array(client_demand)[:, client_idx_list]
    server_name, server_bandwidth = read_server_bandwidth()
    bandwidth = []
    for s in sname:
        idx = server_name.index(s)
        bandwidth.append(server_bandwidth[idx])
    # qos_lim, base_cost = read_qos_limit()
    qos_lim = read_qos_limit()[0]
    base_cost = read_qos_limit()[1]
    center_cost = read_qos_limit()[2]
    bandwidth = np.array(bandwidth)


class OutputAnalyser():
    def __init__(self) -> None:
        self.server_history_bandwidth = []
        self.CENTER_COST_T_SUM=np.zeros(len(time_label),dtype=np.int64) #用于存储每个时刻的总带宽用量
        self.CENTER_COST_T=np.zeros((len(time_label),len(sname)),dtype=np.int64) #用于存储各个时刻每个边缘节点的
        # self.CENTERCOST = np.zeros((len(time_label), len(sname)), dtype=np.int32)
        self.max = len(cname)
        self.curr_time_step = -1
        self.record = np.zeros((len(time_label), len(sname), len(cname)), dtype=np.int32)
        self.tcs_id_record = [[[[] for _ in range(len(sname))] for _ in range(len(cname))] for _ in
                              range(len(time_label))]  # tidx, sidx, cidx -> List[iidx]}
        self.t_s_record = np.zeros((len(time_label), len(sname)), dtype=np.int32)
        self.reset()
        self.webpage_info_init()
        # print(len(time_label))
        self.server_used_bandwidth_T = np.zeros((len(sname),len(time_label)),dtype=np.int64)

    def reset(self):
        self.client_outputed = [False for _ in range(len(cname))] #初始化该时刻客户端的输出标志位
        self.server_used_bandwidth = np.zeros(len(sname), dtype=np.int64)
        self.CENTER_COST=np.zeros((len(sname),stream_kind_count),dtype=np.int64)#s*100,表示该时刻的100个流分别是多少，每时刻初始化
        self.count = 0
        self.curr_time_step += 1

    def webpage_info_init(self):
        self.score1 = 0
        self.score2 = 0
        self._fig_id_list = []
        self._fig_json_list = []


    def output_result(self):
        self.calc_score()
        # score_msg = f'<p>score: {self.score1}</p>'
        # inp = input('generate plot through webpage? y/[n] (default is n):')
        # if inp.strip().lower() == 'n' or inp.strip() == '':
        #     return
        # elif inp.strip().lower() == 'y':
        #     try: self.empty_analyse()
        #     except:
        #         print('your t length is too small to analyze and plot.')
        #         exit(1)
        #     self.plot_manager = PlotManager()
        #     self._analyse_server_history_and_plot()
        #     self.plot_manager.show_webpage(score_msg)
        #     return
        # else:
        #     print('input error, will not plot figure')


    def dispatch_server(self, c_idx: int, s_idx: int, stream_ids: List[str]):
        #客户索引 边缘节点索引 流名字

        stream_id_idxs = [stream_id_map[i] for i in stream_ids]
        # print(stream_id_idxs)



        accu = 0
        for stream_id_idx in stream_id_idxs:
            #判断是否该时刻中心节点该流的消耗的大小，储存大的值
            if self.CENTER_COST[s_idx][stream_id_idx]<client_demand[self.curr_time_step][stream_id_idx][c_idx]:
                self.CENTER_COST[s_idx][stream_id_idx] = client_demand[self.curr_time_step][stream_id_idx][c_idx]
            res = client_demand[self.curr_time_step][stream_id_idx][c_idx]
            self.record[self.curr_time_step, s_idx, c_idx] += res
            self.t_s_record[self.curr_time_step, s_idx] += res
            accu += res#计算这个一共耗费了多少流
            # self.tcs_id_record
            self.server_used_bandwidth[s_idx] += res#存储该时刻该边缘节点使用的带宽
            # print(self.curr_time_step)
            self.server_used_bandwidth_T[s_idx][self.curr_time_step]+=res
            # print(self.curr_time_step)
        if self.server_used_bandwidth_T[s_idx][self.curr_time_step] > bandwidth[s_idx]:#判断带宽是否超过该节点带宽
            err_print(f'bandwidth overflow at server {sname[s_idx]} (index: {s_idx}) \n' \
                      f'{self.count}th line \t time: {time_label[self.curr_time_step]} (index: {self.curr_time_step})',
                      self._curr_read_line)
        if qos[s_idx, c_idx] >= qos_lim:  #判断是否分给了不应该分的节点
            err_print(f'qos larger or equal than qos limit \n' \
                      f'server edge node: {sname[s_idx]} (index: {s_idx}) \t client node: {cname[c_idx]} (index: {c_idx}) \t' \
                      f'{self.count}th line time: {time_label[self.curr_time_step]} (index: {self.curr_time_step})',
                      self._curr_read_line)
        return accu

    def sum_of_client_at_t(self, time_step, cidx) -> float: #算所有客户点在t时刻的需求总和
        sum = 0
        for stream_id, demand in client_demand[time_step].items():
            sum += demand[cidx]
        return sum

    def read_one_line(self, line: str, s_idx):
        # client node process

        try:
            c, remain = line.strip().split(':')
        except:
            err_print('output format error', line)
        c_idx = cname_map.get(c) #看是哪个客户
        if c_idx is None:
            err_print(f'not exists client node: {c}', line)
        if self.client_outputed[c_idx]:#如果说为true 则表示已经输出过 所以会报错
            err_print(f'output format error: the same client node "{c}" appears in the same time \n' \
                      f'or output is not complete (some client demands 0 bandwidth, but you did not output) \n' \
                      f'in the {self._curr_line_idx}th line, time: {time_label[self.curr_time_step]} \n', line)
        else:
            self.client_outputed[c_idx] = True
            self.count += 1
        # server node process
        self.used_stream_id = set()#用过的流id
        client_demand_at_t = self.sum_of_client_at_t(self.curr_time_step, c_idx)#用于储存该时刻的用户总需求
        if remain.strip() == '':
            if client_demand_at_t != 0:
                err_print(f'bandwidth of {cname[c_idx]} is not 0, but did not dispatch edge server')
            self._check_time_step_finished(s_idx)
            return
        dispatchs = remain[1: -1].split('>,<')
        # print(dispatchs)
        if len(dispatchs) == 1:  # only one server 如果只有一个边缘节点 说明数据已经提取好了
            # print(remain[1: -1])
            dispatchs = remain[1: -1].split(',')
            # print(dispatchs)

            if len(dispatchs) == 1:
                err_print('output format error', line) #输出格式错误
            s = dispatchs[0]#边缘节点名称
            ids = dispatchs[1:]#流名称
            res = self._process_server_res(c_idx, s, ids, line)#计算这一行的总流demand
            if int(res) != client_demand_at_t:
                err_print(f'bandwidth of {cname[c_idx]} is not satisfied', line)
            self._check_time_step_finished()
            return
        res_accum = 0
        for d_str in dispatchs:#边缘节点>1 一个一个进行
            str_split = d_str.split(',')
            s = str_split[0]
            ids = str_split[1:]
            res = self._process_server_res(c_idx, s, ids, line)
            res_accum += int(res)#看这一行总共处理的流的带宽
        if res_accum != client_demand_at_t:
            err_print(f'bandwidth accumulation of {cname[c_idx]} is not satisfied', line)
        self._check_time_step_finished()

    def read_one_line_1(self, line: str):#读取百分之一的边缘节点
        # client node process
        try:
            s_1 = line.strip().split(',')
        except:
            err_print('output format error', line)
        s_idx = []
        for i in s_1: #看是哪个边缘节点
            s_idx.append(sname_map.get(i))
        return s_idx


    def _process_server_res(self, c_idx: int, server_name: str, stream_ids: List[str], line: str):
        #客户节点索引 边缘节点的名字 流名字 这一行的数据
        s_idx = sname_map.get(server_name)  # s_idx = sname_map[s]#获取索引
        if s_idx is None:
            err_print(f'not exists edge node: {server_name}', line)
        for id in stream_ids:
            if id in self.used_stream_id:#已用流
                err_print(
                    f'stream id {id} is dispatched more than 2 or more times at time step {time_label[self.curr_time_step]} (line index: {self._curr_line_idx})',
                    line)
            else:
                self.used_stream_id.add(id)
        return self.dispatch_server(c_idx, s_idx, stream_ids)

    def _check_time_step_finished(self,s_idx):#检查这一时刻是否完成
        if self.count == self.max:

            for i in range(len(sname)):
                # if (self.server_used_bandwidth[i] == 0):
                #     continue
                # for j in range(self.curr_time_step+1,len(time_label)):
                if self.curr_time_step==len(time_label)-1:
                    break;
                if i in s_idx:
                    self.server_used_bandwidth_T[i][self.curr_time_step+1]+=math.floor(self.server_used_bandwidth_T[i][self.curr_time_step]*0.01)
                else:
                    self.server_used_bandwidth_T[i][self.curr_time_step+1]+=math.floor(self.server_used_bandwidth_T[i][self.curr_time_step]*0.05)

            # if(self.curr_time_step == 0):
            #     self.server_used_bandwidth_T=self.server_used_bandwidth_T
            # else:
            #     for i in range(len(sname)):
            #         for j in range(self)
            #         self.server_used_bandwidth[self.curr_time_step+1] +=
            self.server_history_bandwidth.append(self.server_used_bandwidth_T[:,self.curr_time_step])#把边缘节点所用带宽存储进去

            # print(self.server_used_bandwidth_T[self.curr_time_step])
            # print(self.CENTER_COST)
            Center_cost_t_SUM=self.CENTER_COST.sum(axis=1)
            # print(Center_cost_t_SUM)
            self.CENTER_COST_T_SUM[self.curr_time_step]=Center_cost_t_SUM.sum()

            # print(self.CENTER_COST_T_SUM)

            # print(self.CENTER_COST)
            self.reset()#重启下一个时刻

    def read_file(self, output_file_name: str):#读取solution的文件内容
        with open(output_file_name) as f:
            lines = f.read().splitlines()
        s_idx = []
        for l1_idx, l1 in enumerate(lines):
            if l1_idx!=0 and l1_idx % len(cname) == 0:
                s_idx_l = []
                s_inx_l_single = l1.split(',')
                for i in s_inx_l_single:
                    s_idx_idx = sname_map.get(i)
                    s_idx_l.append(s_idx_idx)
                s_idx.append(s_idx_l)

        for l_idx, l in enumerate(lines):
            self._curr_read_line = l
            self._curr_line_idx = l_idx
            # if l_idx < len(sname): #第一个时刻
            #     self.read_one_line(l)#逐行读取数据
            # elif l_idx % (len(sname)+1) == 1:
            if l_idx==0 or l_idx % len(cname) != 0 :
                self.read_one_line(l,s_idx[l_idx//len(cname)])

            # if l_idx % len(sname) == 0:
            #     s_idx = self.read_one_line_1(l)
            # if l_idx==0 or l_idx % len(sname) != 0 :
            #     self.read_one_line(l,s_idx)

            # else:
            #     self.read_one_line(l)


        if self.curr_time_step != len(time_label):
            err_print('not all time step is printed')

    def calc_score(self):#计算成绩
        if self.count not in [0, self.max]:#还未完成
            err_print('output is not complete in the last time step')
        time_cnt = len(time_label)#时刻标签
        # print(len(time_label))
        idx = math.ceil(time_cnt * 0.95) - 1#向下取整math.ceil
        idx_100 = time_cnt-1
        idx_75=math.ceil(time_cnt * 0.75) - 1
        idx_50=math.ceil(time_cnt * 0.5) - 1
        idx_25=math.ceil(time_cnt * 0.25) - 1
        idx_0 = 0
        # print(idx)
        server_history = np.array(self.server_history_bandwidth)  # t * s #这个是每一时刻所有的边缘节点所用带宽bian
        # print(server_history)
        sum_all_time = server_history.sum(axis=0)  # s 这个是把该边缘节点的所有时刻需求加起来
        # print(sum_all_time)
        out = []
        server_history.sort(axis=0)#从小到大排序 按照时刻从小到大
        center_cost_history=np.array(self.CENTER_COST_T_SUM)
        center_cost_history.sort()
        # print(center_cost_history)
        # print(server_history)
        score_95 = server_history[idx]  # s  就是把这个时刻的所有边缘节点带宽给取出来
        center_95=center_cost_history[idx] # 把中心节点的百分之95分位提取出来
        # print(center_cost_history)
        # print(center_95)
        for s_idx, sum in enumerate(sum_all_time):
            if sum == 0:
                out.append(0)
                continue
            if score_95[s_idx] <= base_cost:
                out.append(base_cost)
            else:
                this_cost = 1 / (bandwidth[s_idx]) * (score_95[s_idx] - base_cost) ** 2 + score_95[s_idx]
                out.append(round(this_cost))#计算公式
        score = np.array(out).sum()#总和  //这个是边缘节点的
        score_center = center_95*center_cost  #算中间节点成本
        print(f'server final score: {score}\n')
        print(f"center 100 final score: {center_cost_history[idx_100]*center_cost}\n")
        print(f"center 95+1 final score: {center_cost_history[idx+1]*center_cost}\n")
        print(f"center 95 final score: {score_center}\n")
        print(f"center 75 final score: {center_cost_history[idx_75]*center_cost}\n")
        print(f"center 50 final score: {center_cost_history[idx_50]*center_cost}\n")
        print(f"center 25 final score: {center_cost_history[idx_25]*center_cost}\n")
        print(f"center 0 final score: {center_cost_history[idx_0]*center_cost}\n")
        print(f"sum final score: {int(score_center+score+0.5)}\n")
        print(f'separate cost: {out}')


def gauge_time(args):
    start_time = time.time()
    if type(args) is str:
        os.system(args)
    else:
        os.system(' '.join(args))
    end_time = time.time()
    print(f'compile and run time: {(end_time - start_time):.4f}')


if __name__ == '__main__':
    validate_file_exist()
    get_input_data()
    if len(sys.argv) == 1:
        gauge_time('sh build_and_run.sh')
    else:
        gauge_time(sys.argv[1:])
    analyser = OutputAnalyser()
    analyser.read_file(IOFile.output)
    analyser.output_result()

