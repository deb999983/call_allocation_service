# Service Design Note


## Assumptions
1. **Node reports are the source of truth for the call count**. (Originally: all information about nodes should be
   driven from the service. Changed later to accommodate periodic calls)
   Initially I thought that since the node registration is done from the service all changes and updates done 
   on the node after the registration is final and moved ahead with most of the design. I was not taking the `currentCalls`
   into account while registering the node. I thought the `currentCalls` will come into the picture after somebody
   allocates/drops the calls into the nodes.
  
   I changed this assumption later on reading the assignment document more carefully once more and thinking that nodes
   are already created and some other service would be sending the reports to our service. It was a very small change 
   to accommodate the reports, I just treated the reported count as final and kept track of how many calls were allotted/dropped
   since the last report to decide whether or not to place a new call. So now
   ```
   node.currentCallCount = reportedCount + callsSinceLastReport [+1 on allocate, -1 on drop]
   ```
   On receiving a new report, I reset the `callsSinceLastReport`
   ```
   callsSinceLastReport = 0
   ```
2. **CurrentCalls mean the calls which are currently active.**
3. **Call Ids are unique.**
   I assumed the callIds to be globally unique across regions and it is enforced in the `NodeStore`, to simplify the 
   api design. For other cases the api would have been a bit complicated, and it would have been something like,
   - **Unique per node**: `/regions/{region}/nodes/{node_id}/calls/{call_id}`
   - **Unique per region**: `/regions/{region}/calls/{call_id}`

4. **Node ids are unique per region.**
   This was the first thing I assumed and continued ahead with it due to the way the problem was framed in the document 
5. **State Loss after service restart**
   This is implicit from the service requirement as the allocation data is stored in memory.
6. **Calls are dropped if all nodes in a region are full**:
   Initially I assumed to add it to a pending queue on NodeStore and have a background thread try to place them if 
   capacity is available but then dropped the idea because of time.

## Service Layers
- API Layer [Routes and DTOs, translates between Domain and API]
- DomainLayer
  - Call
  - CallNode
- Service Layer 
  - Call Service
  - Node Service
- Database Layer [Translates between database entities and domain]
  - Entities
  - NodeStore 
     Is a Singleton object and every operation is done by obtaining a global lock in the `NodeStore` object to support
     parallel api calls.
- Utils
    - CallPlacer.kt [Call Allocation Logic]
      This could have been in the CallService itself, but I thought I could reuse it in the background thread, but didn't
      implement it.

The above layers were fairly straightforward to decide as I use most of them in our daily work. The one new thing was
writing an `in-memory db` layer with proper synchronisation. The `withLock` pattern I borrowed from the way we do atomic
transactions when interacting with postgres. Although it is a bit different as it locks the whole object where as postgres
locks a single row which is being updated.

One difference to the standard way of doing things is the Entity layer, as in Postgres we convert raw sql rows to domain
objects, but here if we use domain objects for storage and hand it over to service, so it creates the issue of handing 
live store objects to upper layers, and if they changed accidentally it will corrupt the data.  


## Allocation Strategy
The allocation strategy is very simple, it allocates call by the least loaded node relative to its total capacity. This
was the first thought I had and I went ahead with it as it keeps the load balanced. The tradeOff might me the it would
take a long time to scale the nodes down (if there is a check for that somewhere) as opposed to loading one node at a 
time.


## Deployment
The service is deployed to kubernetes with `kind` clusters, as the service needs to run locally and it would have been a big task
to deploy on cloud for this task.

I kept the replicas as `1` and deployment strategy as `Recreate` to stop a pod completely before recreating a pod, 
as the service needs to run on a single instance due to in memory state. The limitations of this strategy is there
would be some down time but it's unavoidable due to the way the service is designed

**MemoryRequest == limit and  MaxRAMPercentage=75**: When JVM applications run in a container the heap is sized from **limit**
and the scheduler looks at the request when assigning pods to a node. If a node has 2GB memory and 4 pods are scheduled
with 512MB request and 1GB limit the heap can potentially increase to more than 2GB at max utilization ie (3GB) and pods
can be killed



## Tools Used
I used Claude Code to write the following
- Unit tests
- scripts/seed-and-report.sh. [Just a smoke test to check everything is running correctly]
- Makefile
- Readme
- Logical commits(Grouping files and commit messages)


## Things I would have liked to do
- Add some end to end tests except the standalone `scripts/seed-and-report.sh`.
- Add a cluster report api to have comprehensive view of all the nodes, running and completed calls etc.
- Instead of dropping the calls on all nodes full in a region, I would have liked to add them to a pending queue to the
  node store and have a background thread query the pendingCalls and try to place them once the capacity is available.
- I could have standardized the register node api call as its GET component is a bit different. 