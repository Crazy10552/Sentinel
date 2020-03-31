#!/usr/bin/env bash
set -e
## 如果脚本启动报错syntax error: unexpected end of file
## 请使用vim编辑脚本，退出编辑模式，输入 :set ff=unix 将脚本格式改为unix
#echo "Usage: xxx.sh {uat|sim|pet|prod} {start|restart|stop}"
#获取语句本身
PRG=$0
#echo "usage ${PRG} {uat|sim|pet|prod} {start|restart|stop}"

#获取环境参数 uat|sim|pet|prod
runEnv=$1

#获取操作类型 start|restart|stop
action_type=$2

#dashboardip
dashboardIp=$3

#dashboardPort
dashboardPort=$4

# 获取脚本当前目录路径
currentDir=$(cd `dirname $0`;pwd)
# 获取脚本当前目录名
dirName=`basename ${currentDir}`

mkdir -p /home/sentinel/logs/$dirName
chmod o+r -R /home/sentinel/logs/$dirName

JAVA_MEMORY="-Xmx256m -Xmn256m"

if [[ ! -n "$runEnv"  ]] ; then
   echo "Usage: $PRG {zjrc|yuntai} {start|restart|stop} dashboardIp dashboardPort"
   exit 0
elif [[  "$runEnv"  = "zjrc"  ]] ; then
    logParentPath="/home/sentinel/logs/csp/"
#   JAVA_MEMORY="-Xmx256m -Xmn256m"
elif [[  "$runEnv"  = "yuntai"  ]] ; then
    logParentPath="/home/logs/sentinel/csp/"
#   JAVA_MEMORY="-Xmx256m -Xmn256m"
else
   echo "Usage: $PRG {zjrc|yuntai} {start|restart|stop} dashboardIp dashboardPort"
   exit 0
fi

if [[ ! -n "${dashboardIp}"  ]] ; then
   dashboardIp="127.0.0.1"
fi
if [[ ! -n "${dashboardPort}"  ]] ; then
   dashboardPort=8809
fi

JAVA_AMP="-Dserver.port=${dashboardPort} -Dcsp.sentinel.dashboard.server=${dashboardIp}:${dashboardPort} -Dcsp.sentinel.log.use.pid=true -Dcsp.sentinel.log.dir=${logParentPath}${dirName}"



case ${action_type} in

  start)
     if [[ -z "${JAVA_HOME}"  ]] ; then
         source /etc/profile
         if [[ -z "${JAVA_HOME}"  ]] ; then
             if [[ -z "${JRE_HOME}"  ]] ; then
                 echo "Please Configure Java environment variables JAVA_HOME"
                 exit 1
             fi
             JAVA_HOME=${JRE_HOME}
         fi
     fi

     if [[ -z "${JAVA_runEnvS}"  ]] ; then
	 # gc 之前会打印堆栈信息,实测一次打印信息较多,慎用
         #JAVA_runEnvS=$JAVA_MEMORY " -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${currentDir}/ -Xloggc:${currentDir}/gc_log_${dirName}.log -XX:+PrintGCDateStamps -XX:+PrintGCDetails -XX:+PrintHeapAtGC -Djava.security.egd=file:/dev/./urandom  -XX:AutoBoxCacheMax=20000 -XX:+PrintCommandLineFlags"

	 # 默认用这个配置
	JAVA_runEnvS=$JAVA_MEMORY" -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${currentDir}/ -Xloggc:${currentDir}/gc_log_${dirName}.log -XX:+PrintGCDateStamps -XX:+PrintGCDetails -Djava.security.egd=file:/dev/./urandom  -XX:AutoBoxCacheMax=20000 -XX:+PrintCommandLineFlags ${JAVA_AMP}"
     fi
     echo "-------------------------------------show start shell and pid --------------------------------------------"
     #echo "nohup ${JAVA_HOME}/bin/java ${JAVA_runEnvS} -jar ${currentDir}/${dirName}.jar --spring.profiles.active=$runEnv >/dev/null 2>&1 &"
     nohup ${JAVA_HOME}/bin/java ${JAVA_runEnvS} -jar ${currentDir}/${dirName}.jar --spring.profiles.active=$runEnv >/dev/null 2>&1 &
     # echo "Service startup success"
     serverpid=`jps -l |grep ${dirName}|awk -F ' ' '{print$1}'`
     echo "${dirName} pid =" $serverpid
     sleep 5s

	 logfilepath=`lsof -anP -p $serverpid |grep logs |grep log|awk -F ' ' '{print$NF}'`
     #前面已经做了判断,这里就不做非空判断
     if [[  "$runEnv"  = "prod"  ]] ; then
		echo "-------------------------------------show log file--------------------------------------------"
		for tmp_file in $logfilepath
		do
		    chmod og+r $tmp_file
		    echo "please input \"tail -fn100 $tmp_file \"view log"
		done
	 else
		 sleep 5s
		 if [ ! -n "$logfilepath" ] ; then
		   #程序启动遇到问题,直接展示java -jar 的相关日志方便查看排查问题
		   ${JAVA_HOME}/bin/java ${JAVA_runEnvS} -jar ${currentDir}/${dirName}.jar --spring.profiles.active=$runEnv
		 else
		   tail -n200 $logfilepath
		 fi
     fi

    ;;

  stop)
    # 获取当前服务的线程号
    echo "-------------------------------------show kill result  --------------------------------------------"
    serverpid=`ps  --no-heading -C java -f --width 1000 | grep "$dirName" |awk '{print $2}'`
    if [[ -z "${serverpid}" ]]; then
        echo "The server $dirName is not started!"
    else
        echo "${dirName} kill -15 ${serverpid}"
    	kill -15 ${serverpid}
		serverpid=`ps  --no-heading -C java -f --width 1000 | grep "$dirName" |awk '{print $2}'`
		count=0
		limitcount=15
		while [[ "$serverpid" ]]
		do
		  let count=$count+1;
		  let limitcount=${limitcount};
		  echo $dirName "serverpid="$serverpid "has not stop yet,waiting second "$count
		  sleep 1s
		  if [[ ${count} -ge ${limitcount} ]];then
			  kill -9 $serverpid
			  echo $dirName "serverpid="$serverpid "can't stop by kill -15 waiting for" ${limitcount} "second then use kill -9 "
		  fi
		  serverpid=`ps  --no-heading -C java -f --width 1000 | grep "$dirName" |awk '{print $2}'`
		done
    fi    
    ;;
	
  restart)
    sh $PRG $runEnv stop $dashboardIp $dashboardPort
    sh $PRG $runEnv start $dashboardIp $dashboardPort
    ;;
	
*)
    echo "Usage: $PRG {zjrc|yuntai} {start|restart|stop} dashboardIp dashboardPort"
    ;;
esac
exit 0

