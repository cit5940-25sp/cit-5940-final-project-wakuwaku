## 1. Movie Game Logic

1. The first movie is randomly generated.  
   **(If a player later enters the same movie as the starter, they must enter a different one.)**

2. Each player selects a **movie genre** from the database as their **long-term win condition**.  
   Both players may choose the same genre.

3. In each round, the movie a player names must be **connected to the opponent’s previous movie**.  
   - **Valid connections include:** shared actor, director, writer, cinematographer, or composer.  
   - The movie **does not need to match the player’s selected genre** in every round.

4. Input rules:
   - **If an invalid movie name (e.g., incorrect title or previously used movie) is entered, prompt the player to re-enter.**
   - **If no movie is entered within the time limit → automatic loss.**
   - **If a valid movie is entered, check if the same type of connection has already been used more than 3 times:**
     - **If so → automatic loss.**

5. A player wins by either:  
   - **Being the first to name 5 movies that match their selected genre**,  
   — or —  
   - **If the opponent fails to name a connected movie within 30 seconds.**

---

> 📌 **Notes:**  
> - **Each movie can only be used once.** Repeated entries are not allowed.  
> - **Each connection type (e.g., actor) can be used a maximum of 3 times.**

## 2. Implemented Optional Feature

#### Add Time Feature

We’ve implemented a creative optional feature called **"Add Time"** to enhance gameplay flexibility.

#### How It Works:
- Each player has **one opportunity** to add 60 seconds to any of their turns.
- The **"Add Time"** status is shown as `"1/1"` at the start of the game, indicating one available use.
- When a player inputs `"++"`, the display updates to `"0/1"` and an additional **60 seconds** is added to that turn.

#### 💡 Reason for Adding This Feature:
During testing, we found that the default 30-second limit was sometimes too short for players to both recall and type in a valid movie title.  
By offering a **one-time extension**, we aim to:
- Create a more balanced game experience  
- Reduce pressure during tough turns  
- Allow for strategic time management


## 3. Design Pattern



## 4. Team Member
