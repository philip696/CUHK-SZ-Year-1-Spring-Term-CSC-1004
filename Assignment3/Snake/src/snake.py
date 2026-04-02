import tkinter as tk
import random
import os
from PIL import Image, ImageTk

class SnakeGame:
    #game constants and variables
    def __init__(self, root):
        self.root = root
        self.root.title("Snake Game")
        
        #game constants
        self.tile_size = 50
        self.grid_width = 10
        self.grid_height = 10
        self.canvas_width = self.grid_width * self.tile_size
        self.canvas_height = self.grid_height * self.tile_size + 40
        
        #game variables
        self.snake = []
        self.direction = "Right"
        self.next_direction = "Right"
        self.food_pos = None
        self.score = 0
        self.high_score = 0
        self.game_speed = 200  #default
        self.game_active = False
        self.paused = False
        
        self.load_specific_images()
        
        self.create_widgets()
        
        #hhow difficulty selection first
        self.show_difficulty_menu()
        root.bind("<Escape>", lambda event: self.show_pause_menu())

    #center function
    def center_window(self, window=None, width=None, height=None):
        """Center any window on the screen"""
        if window is None:
            window = self.root
        if width is None or height is None:
            window.update_idletasks()
            width = window.winfo_width()
            height = window.winfo_height()
        
        #position & dimension
        screen_width = window.winfo_screenwidth()
        screen_height = window.winfo_screenheight()
        x = (screen_width - width) // 2
        y = (screen_height - height) // 2
        window.geometry(f'{width}x{height}+{x}+{y}')
    
    #load image function
    def load_image(self, path, size):
        try:
            #try to open the image file
            img = Image.open(os.path.join("images", path)) 
            img = img.resize(size, Image.LANCZOS)  #resize
            return ImageTk.PhotoImage(img)
        except Exception as e:
            print(f"Error loading image {path}: {e}")
            #fallback to a colored rectangle
            img = Image.new('RGB', size, color='green')
            return ImageTk.PhotoImage(img)
    
    def load_specific_images(self):
        #load all required images (replace these paths with your actual image files)
        try:
            #menu images
            self.snake_img = self.load_image("snake.png", (450, 80))
            self.easy_img = self.load_image("easy.png", (200, 50))
            self.moderate_img = self.load_image("moderate.png", (250, 50))
            self.hard_img = self.load_image("hard.png", (200, 50))
            self.menu_img = self.load_image("menu.png", (175, 50))
            self.difficulty_img = self.load_image("difficulty.png", (250, 40))
            self.quit_img = self.load_image("quit.png", (150, 40))
            self.play_again_img = self.load_image("play_again.png", (200, 50))
            self.game_over_img = self.load_image("game_over.png", (350, 100))
            self.resume_img = self.load_image("resume.png", (200, 50)) 
            
            
            #snake parts
            self.head_up_img = self.load_image("headUp.png", (self.tile_size, self.tile_size))
            self.head_down_img = self.load_image("headDown.png", (self.tile_size, self.tile_size))
            self.head_left_img = self.load_image("headLeft.png", (self.tile_size, self.tile_size))
            self.head_right_img = self.load_image("headRight.png", (self.tile_size, self.tile_size))
            self.body_hor_img = self.load_image("bodyHor.png", (self.tile_size, self.tile_size))
            self.body_ver_img = self.load_image("bodyVer.png", (self.tile_size, self.tile_size))
            self.tail_left_img = self.load_image("tailLeft.png", (self.tile_size, self.tile_size))
            self.tail_right_img = self.load_image("tailRight.png", (self.tile_size, self.tile_size))
            self.tail_up_img = self.load_image("tailUp.png", (self.tile_size, self.tile_size))
            self.tail_down_img = self.load_image("tailDown.png", (self.tile_size, self.tile_size))
            
            #food
            self.apple_img = self.load_image("apple.png", (self.tile_size, self.tile_size))
            self.apple_score_img = self.load_image("apple.png", (40, 40)) 
            self.gapple_score_img = self.load_image("gapple.png", (40, 40)) 

        except:
            #fallback if images dont load
            self.use_images = False
        else:
            self.use_images = True
    
    #create neccesary background graphics
    def create_widgets(self):

        #set canvas
        self.canvas = tk.Canvas(
            self.root, 
            width=self.canvas_width, 
            height=self.canvas_height, 
        )
        self.canvas.pack()

        #create main frame
        self.main_frame = tk.Frame(self.root)
        self.main_frame.pack(expand=True, fill=tk.BOTH)
        
        #create top bar
        self.top_bar = tk.Frame(self.root)
        self.top_bar.pack(fill=tk.X)
        
        # Menu button
        self.menu_button = tk.Button(
            self.top_bar, 
            image=self.menu_img,
            command=self.show_pause_menu,
            highlightthickness=0
        )
        self.menu_button.pack(side=tk.LEFT, padx=5, pady=5)

        self.score_label = tk.Frame(self.top_bar, bg="black")
        self.score_label.pack(side=tk.RIGHT, padx=10)
        self.update_score_display()  #uses default black background        

        self.canvas.pack(expand=True)
        self.main_frame.pack_propagate(False)  #prevent frame from resizing to contents

        self.center_window()
    
    #randomize the placing of food
    def place_food(self):
        while True:
            x = random.randint(0, self.grid_width - 1)
            y = random.randint(0, self.grid_height - 1)
            if (x, y) not in self.snake:
                self.food_pos = (x, y)
                break
    
    #create game graphics
    def draw_game(self):
        self.canvas.delete("all")
        
        #draw grid background with light green and green tiles
        for x in range(self.grid_width):
            for y in range(self.grid_height):
                #create alternating tile pattern
                color = "#88b888" if (x + y) % 2 == 0 else "#a8d8a8"
                self.canvas.create_rectangle(
                    x * self.tile_size, 
                    y * self.tile_size + 40,
                    (x + 1) * self.tile_size,
                    (y + 1) * self.tile_size + 40,
                    fill=color,
                )
        
        #draw food
        if self.food_pos:
            fx, fy = self.food_pos
            self.canvas.create_image(
                fx * self.tile_size, 
                fy * self.tile_size + 40,
                image=self.apple_img,
                anchor=tk.NW
            )
        
        #draw snake
        for i, (x, y) in enumerate(self.snake):
            if i == 0:  #head
                if self.direction == "Up":
                    img = self.head_up_img
                elif self.direction == "Down":
                    img = self.head_down_img
                elif self.direction == "Left":
                    img = self.head_left_img
                else:  #right
                    img = self.head_right_img
            elif i == len(self.snake) - 1:  #tail
                prev_x, prev_y = self.snake[i-1]
                if prev_x < x:  
                    img = self.tail_left_img
                elif prev_x > x: 
                    img = self.tail_right_img
                elif prev_y < y:  
                    img = self.tail_up_img
                else:  
                    img = self.tail_down_img
            else:  #body
                prev_x, prev_y = self.snake[i-1]
                next_x, next_y = self.snake[i+1] if i+1 < len(self.snake) else (x, y)
                
                if (prev_x == next_x) or (prev_y == next_y):  #straight
                    if prev_x != x:  #horizontal
                        img = self.body_hor_img
                    else:  #vertical
                        img = self.body_ver_img
                else:  #corner
                    img = self.body_hor_img  

            #draw it on tile
            if self.use_images:
                self.canvas.create_image(
                    x * self.tile_size, 
                    y * self.tile_size + 40,
                    image=img,
                    anchor=tk.NW
                )
            else:
                color = "darkblue" if i == 0 else "blue"
                self.canvas.create_rectangle(
                    x * self.tile_size, 
                    y * self.tile_size + 40,
                    (x + 1) * self.tile_size,
                    (y + 1) * self.tile_size + 40,
                    fill=color,
                    outline="darkblue"
                )
    
    #game loop function
    def game_loop(self):
        if not self.game_active or self.paused:
            return
        
        #move snake
        self.move_snake()
        
        #check collisions
        if self.check_collision():
            self.game_over()
            return
        
        #check if food eaten
        if self.snake[0] == self.food_pos:
            self.score += 1
            self.update_score()
            self.place_food()
            #dont remove tail to grow the snake
        else:
            #remove tail if no food eaten
            self.snake.pop()
        
        #redraw
        self.draw_game()
        
        #schedule next move
        self.root.after(self.game_speed, self.game_loop)
    
    #difficulty menu
    def show_difficulty_menu(self):
        self.game_active = False
        self.paused = False
        
        #create a popup window
        self.difficulty_window = tk.Toplevel(self.root)
        self.difficulty_window.title("Select Difficulty")
        self.difficulty_window.resizable(False, False)
        self.difficulty_window.configure(bg="#ffffff") 
        
        #center the window
        self.center_window(self.difficulty_window, 550, 350)
        
        #add title image
        title_label = tk.Label(
            self.difficulty_window, 
            image=self.snake_img,
            bg="#ffffff" 
        )
        title_label.pack(pady=(30, 0))
        
        #difficulty buttons with light blue background
        easy_btn = tk.Button(
            self.difficulty_window, 
            image=self.easy_img,
            command=lambda: self.set_difficulty(250),
            borderwidth=2,
            highlightthickness=2,
            highlightbackground="#008000",
            relief=tk.SOLID
        )
        easy_btn.pack(pady=5)
        
        moderate_btn = tk.Button(
            self.difficulty_window, 
            image=self.moderate_img,
            command=lambda: self.set_difficulty(200),
            borderwidth=2,
            highlightthickness=2,
            highlightbackground="#FFD700",
            relief=tk.SOLID
        )
        moderate_btn.pack(pady=5)
        
        hard_btn = tk.Button(
            self.difficulty_window, 
            image=self.hard_img,
            command=lambda: self.set_difficulty(150),
            borderwidth=2,
            highlightthickness=2,
            highlightbackground="#FF0000",
            relief=tk.SOLID
        )
        hard_btn.pack(pady=5)
        
        #make the window modal
        self.difficulty_window.grab_set()
        self.difficulty_window.transient(self.root)
        self.difficulty_window.wait_window()

        self.difficulty_window.after(100, lambda: self.difficulty_window.bind("<Escape>", lambda e: self.difficulty_window.destroy()))
    
    #difficulty set
    def set_difficulty(self, speed):
        self.game_speed = speed
        if hasattr(self, 'difficulty_window'):
            self.difficulty_window.destroy()
        self.start_game()
        
    #show difficulty
    def show_difficulty_from_pause(self):
        if hasattr(self, 'pause_window'):
            self.pause_window.destroy()
        self.show_difficulty_menu()

    #pause menu
    def show_pause_menu(self):
        if not self.game_active or self.paused:
            return
        
        self.paused = True  #pause the game
        
        #create a popup window
        self.pause_window = tk.Toplevel(self.root)
        self.pause_window.title("Paused")
        self.pause_window.resizable(False, False)
        self.pause_window.configure(bg="#ffffff")
        
        #center window
        self.center_window(self.pause_window, 500, 350)

        #resume Button
        resume_btn = tk.Button(
            self.pause_window,
            image=self.resume_img,
            command=self.resume_game,
            highlightthickness=2,
            highlightbackground="#FFFFFF",
            relief=tk.SOLID
        )
        resume_btn.pack(pady=10)
        
        #difficulty button
        difficulty_btn = tk.Button(
            self.pause_window, 
            image=self.difficulty_img,
            command=self.show_difficulty_from_pause,
            highlightthickness=2,
            highlightbackground="#FFFFFF",
            relief=tk.SOLID
        )
        difficulty_btn.pack(pady=10)
        
        #quit button
        quit_btn = tk.Button(
            self.pause_window, 
            image=self.quit_img,
            command=self.quit_game,
            highlightthickness=2,
            highlightbackground="#FFFFFF",
            relief=tk.SOLID
        )
        quit_btn.pack(pady=10)

        score_frame = tk.Frame(self.pause_window, bg="#ffffff")
        score_frame.pack(pady=10)
        self.update_score_display(parent=score_frame, bg_color="#ffffff")
        
        #handle window close
        self.pause_window.protocol("WM_DELETE_WINDOW", self.resume_game)
        
        #make the window modal
        self.pause_window.grab_set()
        self.pause_window.transient(self.root)
        self.pause_window.wait_window()
    
    #quit
    def quit_game(self):
        self.root.destroy()
    
    #game finished
    def game_over(self):
        self.game_active = False
        
        #create a popup window
        self.game_over_window = tk.Toplevel(self.root)
        self.game_over_window.title("Game Over")
        self.game_over_window.resizable(False, False)
        self.game_over_window.configure(bg="#ffffff")  #Light blue background
        
        #center window
        self.center_window(self.game_over_window, 500, 400)
        
        #game over image
        game_over_label = tk.Label(
            self.game_over_window, 
            image=self.game_over_img,
            bg="#ffffff"
        )
        
        game_over_label.pack(pady=20)
        
        #replace score display with:
        score_frame = tk.Frame(self.game_over_window, bg="#ffffff")
        score_frame.pack(pady=10)
        self.update_score_display(parent=score_frame, bg_color="#ffffff")
        
        #play again button
        play_again_btn = tk.Button(
            self.game_over_window, 
            image=self.play_again_img,
            command=self.restart_game,
            highlightthickness=2,
            highlightbackground="#FFFFFF",
            relief=tk.SOLID
        )
        play_again_btn.pack(pady=20)

        quit_btn = tk.Button(
            self.game_over_window, 
            image=self.quit_img,
            command=self.quit_game,
            highlightthickness=2,
            highlightbackground="#FFFFFF",
            relief=tk.SOLID
        )
        quit_btn.pack(pady=10)
        
        #handle window close
        self.game_over_window.protocol("WM_DELETE_WINDOW", self.restart_game)
        
        #make window modal
        self.game_over_window.grab_set()
        self.game_over_window.transient(self.root)
        self.game_over_window.wait_window()

        self.difficulty_window.after(100, lambda: self.difficulty_window.bind("<Escape>", lambda e: self.difficulty_window.destroy()))
    
    #restart
    def restart_game(self):
        if hasattr(self, 'game_over_window'):
            self.game_over_window.destroy()
        self.start_game()
    
    #continue game when pause
    def resume_game(self):
        if hasattr(self, 'pause_window'):
            self.pause_window.destroy()
        self.paused = False
        if self.game_active: 
            self.game_loop()
    
    #key functions
    def on_key_press(self, event):
        if not self.game_active or self.paused:
            return
        
        key = event.keysym
        if (key == "Up" and self.direction != "Down" or
            key == "Down" and self.direction != "Up" or
            key == "Left" and self.direction != "Right" or
            key == "Right" and self.direction != "Left"):
            self.next_direction = key

    #start the game
    def start_game(self):
        self.game_active = True
        self.paused = False
        self.score = 0
        self.update_score()
        
        #initialize snake
        self.snake = [
            (4, 5),  #head
            (3, 5),  #body
            (2, 5)   #tail
        ]
        self.direction = "Right"
        self.next_direction = "Right"
        
        #place first food
        self.place_food()
        
        #draw everything
        self.draw_game()
        
        #start game loop
        self.game_loop()
    
    #movement of snake
    def move_snake(self):
        self.direction = self.next_direction
        head_x, head_y = self.snake[0]
        
        if self.direction == "Up":
            new_head = (head_x, head_y - 1)
        elif self.direction == "Down":
            new_head = (head_x, head_y + 1)
        elif self.direction == "Left":
            new_head = (head_x - 1, head_y)
        elif self.direction == "Right":
            new_head = (head_x + 1, head_y)
        
        self.snake.insert(0, new_head)
    
    #check the collisons against itself/wall
    def check_collision(self):
        head_x, head_y = self.snake[0]
        
        #check wall collision
        if (head_x < 0 or head_x >= self.grid_width or
            head_y < 0 or head_y >= self.grid_height):
            return True
        
        #check self collision (skip the head)
        if (head_x, head_y) in self.snake[1:]:
            return True
        
        return False
    
    #update the score
    def update_score(self):
        if self.score > self.high_score:
            self.high_score = self.score
        self.update_score_display()  #Call the new display method

    #update the display of score
    def update_score_display(self, parent=None, bg_color="black"):
        if parent is None:
            parent = self.score_label
        
        #clear previous widgets
        for widget in parent.winfo_children():
            widget.destroy()
        
        #try to use images
        if self.use_images and hasattr(self, 'apple_score_img'):
            #apple icon (score)
            tk.Label(
                parent,
                image=self.apple_score_img,
                bg=bg_color
            ).pack(side=tk.LEFT)
            
            tk.Label(
                parent,
                text=f": {self.score}  ",
                font=("Arial", 12),
                fg="white" if bg_color == "black" else "black",
                bg=bg_color
            ).pack(side=tk.LEFT)
            
            #golden apple icon (high score)
            tk.Label(
                parent,
                image=self.gapple_score_img,
                bg=bg_color
            ).pack(side=tk.LEFT)
            
            tk.Label(
                parent,
                text=f": {self.high_score}",
                font=("Arial", 12),
                fg="white" if bg_color == "black" else "black",
                bg=bg_color
            ).pack(side=tk.LEFT)

#main program
if __name__ == "__main__":
    root = tk.Tk()
    game = SnakeGame(root)
    
    #bind keyboard events
    root.bind("<Up>", game.on_key_press)
    root.bind("<Down>", game.on_key_press)
    root.bind("<Left>", game.on_key_press)
    root.bind("<Right>", game.on_key_press)
    
    root.mainloop()