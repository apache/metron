---
layout: default
title: Apache Metron Documentation
---

<section class="hero-second-level no-padding">
    <div class="bg-img">
        <img src="/img/metron_datacenter.jpg" alt="UNLOCK THE POWER OF YOUR DATA" style="width: 100%; left: 0px;">
    </div>
    <div class="v-middle-wrapper">
        <div class="v-middle-inner">
            <div class="v-middle">
              <h1>real-time big data security </h1>
            </div>
        </div>
    </div>
</section>

<section class="no-padding">
      <div class="fixed-anchor" style="min-height: 99px;">
        <div class="fixed-links" style="top: 63px; transition: top 0.5s ease;">
            <ul>
                <li class="active"><a href="#quickstart">Quick Start</a></li>
                <li class=""><a href="#installation">Installation</a></li>
                <li class=""><a href="#docshome">DOCS Home</a></li>
            </ul>
        </div>
    </div>  
</section>

<section class="intro-block" id="quickstart">
    <div class="wrapper">
        <div class="text-center">
            <h2>Quick Start</h2>
        </div>
        <div class="content-960 hover-btn text-center">
            <p>The Quick Start installation fully automates the provisioning and deployment of Apache Metron and all necessary prerequisites on a single, virtualized host running on VirtualBox.</p>
            <br>
            <p> This image is designed for quick deployment of a single node Metron cluster running on VirtualBox. This platform is ideal for use by Metron developers. It uses a base image that has been pre-loaded with Ambari and HDP.</p>
            <a class="button-default" href="https://cwiki.apache.org/confluence/display/METRON/Quick+Start" target="_blank">LEARN MORE</a>
        </div>
    </div>
</section>

<section class="events-section info-block darken" id="installation">
    <div class="wrapper"><div class="text-center">
                <h2>Installation</h2>
            </div>
            <div class="col-two text-center">
                <h5 class="bold blue-text">Ansible-based Vagrant single node VM install</h5> <br>      
                <p>This is the best place to play with Metron first. This approach uses the Quick Start installation which automates the provisioning and deployment of Apache Metron on a single, virtualized host running on VirtualBox.</p>
            </div>
            <div class="col-two text-center">
                <h5 class="bold blue-text">Fully automated 10 Node Ansible-based install on AWS using Ambari blueprints and AWS APIs</h5> <br>                
                <p>If you want a more realistic setup of the Metron application, use this approach. This install creates a full-functioning, end-to-end, multi-node cluster running Apache Metron. Keep in mind that this install will spin up 10 m4.xlarge EC2 instances by default.</p>
            </div>
            </div>
<div class="hover-btn text-center">
    			<a class="button-default" href=" https://cwiki.apache.org/confluence/display/METRON/Installation" target="_blank">LEARN MORE</a>
    		</div>
</section>

<section class="intro-block" id="docshome">
    <div class="text-center">
        <h2>DOCS Home</h2>
    </div>
    <div class="content-960 hover-btn text-center">
        <p> Apache Metron documentation provides information on quickly getting started with Metron, performing a full installation, adding additional data sources, and using Metron to triage alerts. Metron documentation is currently a work in progress. Please check back again later as we continue to grow this documentation set.</p>
        <a class="button-default" href=" https://cwiki.apache.org/confluence/display/METRON/Documentation" target="_blank">LEARN MORE</a>
    </div>
</section>