# Load data
this.dir <- dirname(parent.frame(2)$ofile)
setwd(this.dir)

marketdata = read.csv("c2k1800_net_pop1_dos0_v2.csv")

# Set Upstream Transmission parameters, to be removed once the parameters are passed in the data file
trans_cap <- 1800
trans_cost <- 2
group_num <- 2
downstream_data <- data.frame(group=integer(),p=integer(),min=integer(),max=integer())
upstream_data <- data.frame(group=integer(),p=integer(),min=integer(),max=integer())
#root_data <- data.frame(p=integer(),min=integer(),max=integer())

# Keep only "base" nodes, ie. keeping only raw data all traders
traderdata=marketdata[marketdata[,"tag"]=="base", ]

# dropping unused variables
traderdata$tag <- NULL
traderdata$dos <- NULL
traderdata$sd_type <- NULL
traderdata$load <- NULL
traderdata$elast <- NULL
traderdata$steps <- NULL
traderdata$pop <- NULL

#Transform data into four columns, id-price-qmin-qmax
n<-(ncol(traderdata)-1)/3
for (i in 1:n){
  j<- i*3-1
  k<- i*3+1
  temp<-traderdata[c(1,j:k)]
  names(temp) <- c("id","p","min","max")
  if(i == 1 ) {
    pq_data<-temp
  }
  else {
    pq_data<-rbind(pq_data,temp)
  }
}
# drop all the NAs in data
pq_data<-pq_data[complete.cases(pq_data), ]

#Calculate for each group
for(g in 1:group_num)
{
  # generate detailed data of Q for each existing P
  pq_group<-pq_data[pq_data[,"id"]>(g-1)*100 & pq_data[,"id"]<g*100+1, ]
  
  p_list<-unique(pq_group[,"p"]) # list all the P that exist
  p_listasc<-sort(p_list) # Ascending order of all P
  p_listdsc<-sort(p_list,decreasing = TRUE) # Descending order of all P
  id_list<-unique(pq_group[,"id"]) # list all the id that exist
  id_list<-sort(id_list) # sort id_list in ascending order
  pmax <- max(p_list) # Find the Max Price
  pmin <- min(p_list) # Find the Min Price
  
  pq_sliced<-data.frame(id=integer(),p=integer(),min=integer(),max=integer())
  # For each id
  for (m in id_list) { 
    # build a temporary dataframe for this id and fetch all the data of horizontal stpes
    pq_m <- subset(pq_group,id==m)
    # find the Q for Pmax
    if(any(pq_m[,"p"]==pmax)==FALSE) {
      q0 <- pq_m[pq_m[,"p"]==max(pq_m[,"p"]),"min"]
      pq_m[nrow(pq_m) + 1,] <- c(m,pmax,q0,q0)
    }
    # for each P from Max to Min, find the Q
    for (x in p_listdsc) {
      # if this p already exist, then skip; if not, fill qmin=qmax=lastqmax
      if(any(pq_m[,"p"]==x)==FALSE) {
        pq_m[nrow(pq_m) + 1,] <- c(m,x,lastqmax,lastqmax)
      }
      lastqmax <- pq_m[pq_m[,"p"]==x,"max"]
      # when reaching the min p, merge data of this p to the complete dataframe of all P
      if(x==pmin) {
        pq_sliced<-rbind(pq_sliced,pq_m)
      }
    }
  }
  
  # generate downstream data for the group
  for (y in p_listasc) {
    downstream_data[nrow(downstream_data) + 1,] <- c(g,y,sum(pq_sliced[pq_sliced[,"p"]==y,"min"]),sum(pq_sliced[pq_sliced[,"p"]==y,"max"]))
  }
  
  # generate upstream data for the group
  for (y in p_listasc) {
    upstream_vec_y <- downstream_data[downstream_data[,"p"]==y,]
    qmin_y <- upstream_vec_y [upstream_vec_y[,"group"]==g,"min"]
    qmax_y <- upstream_vec_y [upstream_vec_y[,"group"]==g,"max"]
  
    # if qmin>0, buy bid
    if (qmin_y>=0 && qmin_y<trans_cap) {
      upstream_data[nrow(upstream_data) + 1,] <- c(g,y-trans_cost,qmin_y,min(qmax_y,trans_cap))
      #upstream_data<-rbind(upstream_data, c(g,y-trans_cost,qmin_y,min(qmax_y,trans_cap)))
    }
    
    # if qmin<0 and qmax>0
    if (qmin_y<0 && qmax_y>0) {
      #buy 0~qmax
      upstream_data[nrow(upstream_data) + 1,] <- c(g,y-trans_cost,0,min(qmax_y,trans_cap))
      #upstream_data<-rbind(upstream_data, c(g,y-trans_cost,0,min(qmax_y,trans_cap)))
      #sell qmin~0
      upstream_data[nrow(upstream_data) + 1,] <- c(g,y+trans_cost,max(qmin_y,-1*trans_cap),0)
      #upstream_data<-rbind(upstream_data, c(g,y+trans_cost,max(qmin_y,-1*trans_cap),0))
    }
    
    # if qmax<0, sell bid
    if (qmax_y<=0 && qmax_y>-1*trans_cap) {
      upstream_data[nrow(upstream_data) + 1,] <- c(g,y+trans_cost,max(qmin_y,-1*trans_cap),qmax_y)
      #upstream_data<-rbind(upstream_data, c(g,y+trans_cost,max(qmin_y,-1*trans_cap),qmax_y))
    }
  }
}


#Calculate root market
p_root_list <- unique(upstream_data[,"p"])
p_root_listasc<-sort(p_root_list)
root_data <-data.frame(p=p_root_listasc,min=integer(length(p_root_list)),max=integer(length(p_root_list)))
for (p in p_root_listasc) {
  for (g in 1:group_num) {
    #fetch upstream data of group g
    upstream_group_data<-upstream_data[upstream_data["group"]==g,]
    
    #if current < pmin of group, than buy at trans_cap
    if(p<min(upstream_group_data[,"p"])) {
      root_data[root_data[,"p"]==p,"min"] <- root_data[root_data[,"p"]==p,"min"] + trans_cap
      root_data[root_data[,"p"]==p,"max"] <- root_data[root_data[,"p"]==p,"max"] + trans_cap
    }
    #else if current > pmax of group, than sell at trans_cap
    else if(p>max(upstream_group_data[,"p"])) {
      root_data[root_data[,"p"]==p,"min"] <- root_data[root_data[,"p"]==p,"min"] - trans_cap
      root_data[root_data[,"p"]==p,"max"] <- root_data[root_data[,"p"]==p,"max"] - trans_cap
    }
    #else, if current p is within pmin~pmax, than use upstream buy/sell quantity
    else {
      root_data[root_data[,"p"]==p,"min"] <- root_data[root_data[,"p"]==p,"min"] + upstream_group_data[upstream_group_data[,"p"]==p,"min"]
      root_data[root_data[,"p"]==p,"max"] <- root_data[root_data[,"p"]==p,"max"] + upstream_group_data[upstream_group_data[,"p"]==p,"max"]
    }
    
  }
}

#generate ouotput dataframe for downstream
ncol_max <- integer()
for(g in 1:group_num) {
  downstream_group_data <- downstream_data[downstream_data[,"group"]==g,]
  n <- nrow(downstream_group_data)
  ncol_max <- max(ncol_max,n)
}
downstream_output <- data.frame(matrix(NA, nrow = group_num, ncol = ncol_max*3))

for(g in 1:group_num) {
  #fetch only in group g
  downstream_group_data <- downstream_data[downstream_data[,"group"]==g,]
  n <- nrow(downstream_group_data)
  for (i in 1:n) {
    downstream_output[g,3*i-2] <- downstream_group_data[i,"p"]
    downstream_output[g,3*i-1] <- downstream_group_data[i,"min"]
    downstream_output[g,3*i] <- downstream_group_data[i,"max"]
  }
}

#generate ouotput dataframe for upstream
ncol_max <- integer()
for(g in 1:group_num) {
  upstream_group_data <- upstream_data[upstream_data[,"group"]==g,]
  n <- nrow(upstream_group_data)
  ncol_max <- max(ncol_max,n)
}
upstream_output <- data.frame(matrix(NA, nrow = group_num, ncol = ncol_max*3))

for(g in 1:group_num) {
  #fetch only in group g
  upstream_group_data <- upstream_data[upstream_data[,"group"]==g,]
  n <- nrow(upstream_group_data)
  for (i in 1:n) {
    upstream_output[g,3*i-2] <- upstream_group_data[i,"p"]
    upstream_output[g,3*i-1] <- upstream_group_data[i,"min"]
    upstream_output[g,3*i] <- upstream_group_data[i,"max"]
  }
}

#generate ouotput dataframe for root
n <- nrow(root_data)
root_output <- data.frame(matrix(NA, nrow=1, ncol = n*3))
for (i in 1:n) {
  root_output[1,3*i-2] <- root_data[i,"p"]
  root_output[1,3*i-1] <- root_data[i,"min"]
  root_output[1,3*i] <- root_data[i,"max"]
}

#write output files
write.csv(downstream_output,file = "downstream.csv")
write.csv(upstream_output,file = "upstream.csv")
write.csv(root_output,file = "root.csv")