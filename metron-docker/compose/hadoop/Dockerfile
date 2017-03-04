FROM openjdk:7

ENV HADOOP_PREFIX=/opt/hadoop-2.7.3
ENV HADOOP_CONF_DIR=$HADOOP_PREFIX/etc/hadoop
ENV HADOOP_IDENT_STRING=root

RUN curl -sL http://archive.apache.org/dist/hadoop/core/hadoop-2.7.3/hadoop-2.7.3.tar.gz | tar -xzC /opt

COPY ./conf/* $HADOOP_CONF_DIR/
RUN mkdir -p /hadoop/dfs/name \
    && mkdir -p /hadoop/dfs/data \
    && $HADOOP_PREFIX/bin/hdfs namenode -format

WORKDIR $HADOOP_PREFIX
EXPOSE 9000 50010 50020 50070 50075

COPY ./docker-entrypoint.sh /
ENTRYPOINT ["/docker-entrypoint.sh"]
