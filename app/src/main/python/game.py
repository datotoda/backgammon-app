import numpy as np
import gym
import gym_backgammon
from gym_backgammon.envs.backgammon import WHITE, BLACK, COLORS, TOKEN

# env = gym.make('gym_backgammon:backgammon-v0')
# def get_actions():
#
#     # Simulate a dice roll
#     die_1, die_2 = np.random.randint(1, 7, size=2)
#     roll = (die_1, die_2)
#
#     # Reset the environment
#     env.reset()
#
#     # Get valid actions
#     actions = env.get_valid_actions(roll)
#     print(actions)
#     # Return dice roll and actions
#     return roll, actions

class Game:
    def __init__(self):
        self.env  = gym.make('gym_backgammon:backgammon-v0')
        agent_color, first_roll, observation = self.env.reset()
        print("first observ")
        print(observation)

    def get_valid_actions(self, die_1: int, die_2: int) -> list:
        roll = (die_1,die_2)
        actions = self.env.get_valid_actions(roll)
        actions_list = []
        for pair in actions:
            pair_list = []
            for dice in pair:
                pair_list.append(list(dice))
            actions_list.append(pair_list)
        return actions_list

    def get_action_outcome_states(self, valid_actions:list) -> list:
        best_action = None
        observations = []
        if valid_actions:
            valid_actions = list(valid_actions)

        tmp_counter = self.env.counter
        self.env.counter = 0
        state = self.env.game.save_state()

        # Iterate over all the legal moves and pick the best action
        for i, action in enumerate(valid_actions):
            actions_list  = []

            for i in range(len(action)):
                actions_list.append(tuple(action[i]))
            actions_list = tuple(actions_list)

            observation, reward, done, info = self.env.step(actions_list)
            observations.append(observation)

            # restore the board and other variables (undo the action)
            self.env.game.restore_state(state)
        self.env.counter = tmp_counter
        return observations

    def make_step(self, action):
        '''
        action is list of lists with integers, looks like this: [[16, 11], [16, 15]]
        makes move on environment, should be turned into tuple of tuples before passing to environment
        '''
        if not isinstance(action, list):
            action = list(action)


        action = tuple([tuple(pair) for pair in action])

        observation_next, reward, done, winner = self.env.step(action)

        if done:
            print(f"winner:{winner}")

        return observation_next
