
![Logo](https://i.imgur.com/D23Et93.png)

# TrotiUM
## A Scooter Renting Service


## Authors

- [@Pedro Ferreira](https://www.github.com/Pmrf555)
- [@Miguel Gomes](https://www.github.com/MayorX500)
- [@Tiago Carneiro](https://www.github.com/Tiago5Carneiro)
- [@Filipa Gomes](https://www.github.com/FilipaSGomes)


## Run Locally

Clone the project

```bash
  git clone https://github.com/Pmrf555/TrabalhoSD
```

Go to the project directory

```bash
  cd TrabalhoSD
  cd trotiUM/src
```

Start the Server

```bash
  sh Server.sh
```

Start the Client

```bash
  sh Client.sh
```


## FAQ

#### Grade:

null

#### Objective:

The requested objective is the implementation of a platform for managing a fleet of electric scooters.
The platform should be in the form of a client-server system in Java using sockets and threads.
The main purpose of the service is to allow users to reserve and park scooters at different locations.
The map is a grid of N x N locations, where N is a positive integer.
The geographic coordinates of the locations are discrete even indices.
To calculate the distance between two locations, the L1 norm, also known as the Manhattan distance, is used.
To ensure a good distribution of scooters across the map, there is a reward system in place.
This reward system rewards users for taking scooters from location A to location B.
At any given time, there is a list of rewards available, which is updated over time.
Each reward is identified by an origin-destination pair and has an associated reward value calculated based on the distance to be traveled.


## Message Protocols

- OK - Server Response;
- ERROR - Server Response;
- REGISTER - Registers a user to the Server;
- LOGIN - Logs in a user to the Server;
- LIST_SCOOTERS - Shows all scooters in X range of the user
- RESERVE_SCOOTER - Starts a trip, locking a scooter to a user.
- PARK_SCOOTER - Parks a scooter in the determined position;
- KILL - If the user requesting is the `admin` server turns off;
- GET_PROFILE - Used to update the profile of a user from the server to the client;
- SET_PROFILE - Used to update the profile of a user from the client to the server;
- GET_PICKUP_REWARDS - Internal protocol, used to ask for the Pickup Rewards if user is Subscribed to the Rewards function; 
- GET_DROP_REWARDS - Internal protocol, used to ask for the Drop Rewards if user is Subscribed to the Rewards function;
- UPDATE_POSITION - Internal protocol, used to update the users location in the server.
## Documentation

- [Project Script](trotiUM/resources/Script.pdf)
- [Project Report](trotiUM/resources/202223_LEI_SD.pdf)


## Demo
[![DEMO](https://img.youtube.com/vi/3hpco_6C0Cc/0.jpg)](https://www.youtube.com/watch?v=3hpco_6C0Cc)
