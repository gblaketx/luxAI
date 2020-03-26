# Version Notes

## Monty

### Version 1
##### 1.00
Basic Monte Carlo Tree Search on Attack Phase only using handmade heuristic function not yet tuned

Boscoe Killbot Eval:
Data: wins_0.csv
Players: ['Boscoe_1.00' 'Killbot_1.00' 'Monty_1.00']
Monty_1.00 proportion wins: 0.260
Monty_1.00 STDEV wins: 0.439

##### 1.01
Hand-tuned reward step and magnitude of explore bonus
Increased number of simulations to 100 and decreased depth to 10
Added heuristic initializer for child node values

Boscoe Killbot Eval:
Data: wins_2.csv
Players: ['Boscoe_1.00' 'Killbot_1.00' 'Monty_1.01']
Monty_1.01 proportion wins: 0.307
Monty_1.01 STDEV wins: 0.461