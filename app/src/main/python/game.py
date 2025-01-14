import gym
from random import randint
from gym_backgammon.envs.backgammon import Backgammon, WHITE, BLACK, COLORS, TOKEN


class Game:
    def __init__(self):
        self.env  = gym.make('gym_backgammon:backgammon-v0')
#         self.agent_color, self.first_roll, observation = self.env.reset()

#         roll the dice
        roll = randint(1, 6), randint(1, 6)

        self.env.current_agent = BLACK

        self.env.game = Backgammon()
        self.env.counter = 0

        self.agent_color, self.first_roll, observation = self.env.current_agent, roll, self.env.game.get_board_features(self.env.current_agent)

        self.start_color = WHITE if observation[-1] == 0 else BLACK

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

        action = self.replace_24_26_with_bar_off(action)

        action = tuple([tuple(pair) for pair in action])

        observation_next, reward, done, winner = self.env.step(action)

        self.env.get_opponent_agent()

        return observation_next

    def replace_24_26_with_bar_off(self, nested_list):
        for i in range(len(nested_list)):
            if isinstance(nested_list[i], list):
                self.replace_24_26_with_bar_off(nested_list[i])
            elif nested_list[i] in [24, 25]:
                nested_list[i] = 'bar'
            elif nested_list[i] == 26:
                nested_list[i] = -1
            elif nested_list[i] == 27:
                nested_list[i] = 24
        return nested_list
