FROM teamgrit/grit-docker-base
COPY build/install/GRIT GRIT
EXPOSE 8080 6001
RUN cd GRIT
ENTRYPOINT ["bash", "GRIT/runscript.sh"]

