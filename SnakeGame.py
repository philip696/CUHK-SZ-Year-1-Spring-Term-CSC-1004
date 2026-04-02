import tkinter as tk
from tkinter import messagebox
import random
import os
from PIL import Image, ImageTk

class SnakeGame:
    def __init__(self, master):
        self.master = master
        self.master.title("Snake Game")
        
        # Game constants
        self.TILE_SIZE = 40
        self.GRID_WIDTH = 10
        self.GRID_HEIGHT = 10
        self.CANVAS_WIDTH = self.GRID_WIDTH * self.TILE_SIZE
        self.CANVAS_HEIGHT = self.GRID_HEIGHT * self.TILE_SIZE
        self.GAME_SPEED = 200  # Default speed
        self.BORDER_HEIGHT = 50  # Height of the top border
        self.HIGH_SCORE_FILE = "highscore.txt"
        
        # Set window size
        self.master.geometry(f"{self.CANVAS_WIDTH+4}x{self.CANVAS_HEIGHT+self.BORDER_HEIGHT+4}")
        self.master.resizable(False, False)
        
        # Colors
        self.colors = {
            "bg_dark": "#88b888",
            "bg_light": "#a8d8a8",
            "snake_head": "#a8e6cf",
            "snake_body": "#dcedc1",
            "snake_tail": "#ffd3b6",
            "food": "#ffaaa5",
            "border": "#a8e6cf",
            "text": "#555555",
            "popup_bg": "#ffffff",
            "button_bg": "#a8e6cf",
            "button_hover": "#dcedc1",
            "button_text": "#555555",
            "button_outline": "#555555",
            "top_border": "#a8e6cf",
            "menu_bg": "#ffffff",
            "menu_button": "#f0f0f0"
        }
        
        # High score
        self.high_score = self.load_high_score()
        
        # Create top border
        self.border_frame = tk.Frame(self.master, bg=self.colors["top_border"], 
                                   height=self.BORDER_HEIGHT, width=self.CANVAS_WIDTH)
        self.border_frame.pack_propagate(False)
        self.border_frame.pack()
        
        # Load images
        self.button_images = {
            "menu": ImageTk.PhotoImage(Image.open("menu.png").resize((40, 40), Image.LANCZOS))
        }

        self.button_images = {
            "menu": self.load_image("menu.png", 120, 40),
            "score": self.load_image("score.png", 50, 15),
            "high_score": self.load_image("high_score.png", 75, 15),
            "easy": self.load_image("easy.png", 200, 40),
            "moderate": self.load_image("moderate.png", 120, 40),
            "hard": self.load_image("hard.png", 200, 40),
            "difficulty": self.load_image("difficulty.png", 120, 40),
            "quit": self.load_image("quit.png", 200, 40),
            "play_again": self.load_image("play_again.png", 120, 40)
        }

        self.images = {}
        try:
            # Load all directional images
            directions = ['Up', 'Down', 'Left', 'Right']
            
            # Head images
            for direction in directions:
                img_name = f"head{direction}.png"
                if os.path.exists(img_name):
                    self.images[f"head_{direction.lower()}"] = tk.PhotoImage(file=img_name).subsample(1, 1)
            
            # Tail images
            for direction in directions:
                img_name = f"tail{direction}.png"
                if os.path.exists(img_name):
                    self.images[f"tail_{direction.lower()}"] = tk.PhotoImage(file=img_name).subsample(1, 1)
            
            # Body images
            self.images["body_hor"] = tk.PhotoImage(file="bodyHor.png").subsample(1, 1)
            self.images["body_ver"] = tk.PhotoImage(file="bodyVer.png").subsample(1, 1)
            
            # Food image
            self.images["food"] = tk.PhotoImage(file="apple.png").subsample(8, 8)

           
            
        except Exception as e:
            print(f"Error loading images: {e}. Using colored shapes instead")
            self.images = None
        
        # Create UI elements
        self.create_border_ui()
        self.canvas = tk.Canvas(self.master, bg=self.colors["bg_dark"], 
                               width=self.CANVAS_WIDTH, height=self.CANVAS_HEIGHT,
                               highlightthickness=0)
        self.canvas.pack(padx=2, pady=2)
        
        # Game state
        self.snake = []
        self.direction = "Right"
        self.next_direction = "Right"
        self.food = None
        self.score = 0
        self.game_running = False
        self.game_paused = False
        self.menu_open = False
        self.difficulty_popup = None
        
        # Initialize game
        self.create_background()
        self.show_start_screen()
    
    def load_image(self, filename, width, height):
        """Helper method to load and resize images"""
        try:
            img = Image.open(filename)
            img = img.resize((width, height), Image.LANCZOS)
            return ImageTk.PhotoImage(img)
        except:
            print(f"Could not load image: {filename}")
            return None

    def create_border_ui(self):
        """Create UI elements in the top border with image buttons"""
        # Score display (now using image)
        self.score_btn = tk.Button(self.border_frame, 
                                image=self.button_images["score"],
                                borderwidth=0, bg=self.colors["top_border"])
        self.score_btn.pack(side=tk.LEFT, padx=20)
        
        # High score display (now using image)
        self.high_score_btn = tk.Button(self.border_frame, 
                                    image=self.button_images["high_score"],
                                    borderwidth=0, bg=self.colors["top_border"])
        self.high_score_btn.pack(side=tk.LEFT, padx=20)
        
        # Menu button
        self.menu_btn = tk.Button(self.border_frame, 
                                image=self.button_images["menu"],
                                borderwidth=0, bg=self.colors["top_border"],
                                command=self.toggle_menu)
        self.menu_btn.pack(side=tk.RIGHT, padx=20)
    
    def show_difficulty_menu(self):
        """Show difficulty selection menu"""
        if not self.game_running or self.game_paused:
            self.show_start_screen()

    def toggle_menu(self):
        """Toggle the game menu"""
        if not self.game_running:
            return
            
        self.menu_open = not self.menu_open
        
        if self.menu_open:
            self.game_paused = True
            self.show_menu()
        else:
            self.game_paused = False
            self.hide_menu()
            if not self.game_paused:
                self.game_loop()

    def show_menu(self):
        """Display the game menu"""
        self.menu_window = tk.Toplevel(self.master)
        self.menu_window.title("Menu")
        self.menu_window.geometry("200x250")
        self.menu_window.resizable(False, False)
        self.menu_window.protocol("WM_DELETE_WINDOW", self.toggle_menu)
        self.menu_window.transient(self.master)
        self.menu_window.grab_set()
        
        # Center the menu window
        x = self.master.winfo_x() + (self.CANVAS_WIDTH - 200) // 2
        y = self.master.winfo_y() + (self.CANVAS_HEIGHT - 250) // 2
        self.menu_window.geometry(f"200x250+{x}+{y}")
        
        # Menu content
        tk.Label(self.menu_window, text="GAME MENU", font=("Arial", 16, "bold")).pack(pady=10)
            
        # Difficulty button
        if self.button_images["difficulty"]:
            diff_btn = tk.Button(self.menu_window, image=self.button_images["difficulty"],
                            borderwidth=0, command=self.show_difficulty_popup)
            diff_btn.pack(pady=5)
        
        # Quit button
        if self.button_images["quit"]:
            quit_btn = tk.Button(self.menu_window, image=self.button_images["quit"],
                            borderwidth=0, command=self.quit_game)
            quit_btn.pack(pady=5)
        
        # High score display
        tk.Label(self.menu_window, text=f"High Score: {self.high_score}", 
                font=("Arial", 12)).pack(pady=10)

    def hide_menu(self):
        """Hide the game menu"""
        if hasattr(self, 'menu_window') and self.menu_window:
            self.menu_window.destroy()
        self.menu_open = False
    
    def show_difficulty_popup(self):
        """Show difficulty selection popup"""
        self.hide_menu()
        
        self.difficulty_popup = tk.Toplevel(self.master)
        self.difficulty_popup.title("Select Difficulty")
        self.difficulty_popup.geometry("200x200")
        self.difficulty_popup.resizable(False, False)
        self.difficulty_popup.protocol("WM_DELETE_WINDOW", self.close_difficulty_popup)
        self.difficulty_popup.transient(self.master)
        self.difficulty_popup.grab_set()
        
        # Center the popup
        x = self.master.winfo_x() + (self.CANVAS_WIDTH - 200) // 2
        y = self.master.winfo_y() + (self.CANVAS_HEIGHT - 200) // 2
        self.difficulty_popup.geometry(f"200x200+{x}+{y}")
        
        # Popup content
        tk.Label(self.difficulty_popup, text="SELECT DIFFICULTY", 
                font=("Arial", 14, "bold")).pack(pady=10)
        
        difficulties = [
            ("easy", 250),
            ("moderate", 175),
            ("hard", 100)
        ]
        
        for label, speed in difficulties:
            btn_img = self.button_images[label]
            if btn_img:
                btn = tk.Button(self.difficulty_popup, image=btn_img, borderwidth=0,
                            command=lambda s=speed: self.set_difficulty(s))
                btn.pack(pady=5)

    def close_difficulty_popup(self):
        """Close the difficulty popup"""
        if self.difficulty_popup:
            self.difficulty_popup.destroy()
        self.difficulty_popup = None
        self.toggle_menu()  # Reopen main menu

    def set_difficulty(self, speed):
        """Set game difficulty"""
        self.GAME_SPEED = speed
        self.close_difficulty_popup()
        self.toggle_menu()  # Close menu
        self.game_paused = False
        self.game_loop()  # Resume game

    def load_high_score(self):
        """Load high score from file"""
        try:
            if os.path.exists(self.HIGH_SCORE_FILE):
                with open(self.HIGH_SCORE_FILE, "r") as f:
                    return int(f.read())
        except:
            pass
        return 0

    def save_high_score(self):
        """Save high score to file"""
        try:
            with open(self.HIGH_SCORE_FILE, "w") as f:
                f.write(str(self.high_score))
        except:
            pass

    def update_high_score(self):
        """Update high score if current score is higher"""
        if self.score > self.high_score:
            self.high_score = self.score
            self.high_score_label.config(text=f"High Score: {self.high_score}")
            self.save_high_score()

    def quit_game(self):
        """Quit the game with confirmation"""
        if messagebox.askyesno("Quit Game", "Are you sure you want to quit?"):
            self.master.destroy()

    def create_background(self):
        """Create checkerboard background with pastel colors"""
        for row in range(self.GRID_HEIGHT):
            for col in range(self.GRID_WIDTH):
                x1 = col * self.TILE_SIZE
                y1 = row * self.TILE_SIZE
                x2 = x1 + self.TILE_SIZE
                y2 = y1 + self.TILE_SIZE
                
                color = self.colors["bg_light"] if (row + col) % 2 == 0 else self.colors["bg_dark"]
                self.canvas.create_rectangle(x1, y1, x2, y2, fill=color, outline="")

    def create_food(self):
        """Create food at random position"""
        while True:
            col = random.randint(0, self.GRID_WIDTH-1)
            row = random.randint(0, self.GRID_HEIGHT-1)
            food_pos = (col * self.TILE_SIZE, row * self.TILE_SIZE)
            
            snake_tiles = [(x//self.TILE_SIZE, y//self.TILE_SIZE) for (x,y) in self.snake]
            food_tile = (col, row)
            
            if food_tile not in snake_tiles:
                break
                
        center_x = food_pos[0] + self.TILE_SIZE//2
        center_y = food_pos[1] + self.TILE_SIZE//2
        
        # Clear any existing food
        self.canvas.delete("food")
        
        # Try to use image first, fallback to drawing
        if "food" in self.images and self.images["food"]:
            self.canvas.create_image(center_x, center_y, image=self.images["food"], tag="food")
        else:
            # Draw simple food graphic
            apple_size = self.TILE_SIZE // 3
            self.canvas.create_oval(
                center_x-apple_size, center_y-apple_size, 
                center_x+apple_size, center_y+apple_size, 
                fill=self.colors["food"], outline=self.colors["food"], width=1, tag="food"
            )
            # Draw stem
            self.canvas.create_line(
                center_x, center_y-apple_size,
                center_x-3, center_y-apple_size-5,
                fill="#8d6e63", width=2, tag="food"
            )
            
        return food_pos

    def change_direction(self, event):
        """Handle direction changes with prevention of 180-degree turns"""
        key = event.keysym
        if (key == "Left" and self.direction != "Right" or
            key == "Right" and self.direction != "Left" or
            key == "Up" and self.direction != "Down" or
            key == "Down" and self.direction != "Up"):
            self.next_direction = key

    def move_snake(self):
        """Move the snake with accurate food collision detection"""
        self.direction = self.next_direction
        head_x, head_y = self.snake[0]
        
        if self.direction == "Left":
            new_head = (head_x - self.TILE_SIZE, head_y)
        elif self.direction == "Right":
            new_head = (head_x + self.TILE_SIZE, head_y)
        elif self.direction == "Up":
            new_head = (head_x, head_y - self.TILE_SIZE)
        elif self.direction == "Down":
            new_head = (head_x, head_y + self.TILE_SIZE)
        
        head_col = new_head[0] // self.TILE_SIZE
        head_row = new_head[1] // self.TILE_SIZE
        food_col = self.food[0] // self.TILE_SIZE
        food_row = self.food[1] // self.TILE_SIZE
        
        self.snake.insert(0, new_head)
        
        if head_col == food_col and head_row == food_row:
            self.score += 1
            self.canvas.delete("food")
            self.food = self.create_food()
            self.flash_effect(new_head[0], new_head[1])
        else:
            self.snake.pop()

    def flash_effect(self, x, y):
        """Create a brief flash effect when food is eaten"""
        center_x = x + self.TILE_SIZE//2
        center_y = y + self.TILE_SIZE//2
        
        def create_circle(size, alpha):
            circle = self.canvas.create_oval(
                center_x-size, center_y-size,
                center_x+size, center_y+size,
                outline=f"#{int(180*alpha):02x}{int(230*alpha):02x}{int(200*alpha):02x}",
                width=2,
                tags="flash"
            )
            return circle
        
        circles = []
        for i, (size, alpha) in enumerate([(10, 0.8), (15, 0.6), (20, 0.4), (25, 0.2)]):
            self.master.after(i*50, lambda s=size, a=alpha: circles.append(create_circle(s, a)))
        
        self.master.after(250, lambda: self.canvas.delete("flash"))

    def draw_snake(self):
        """Draw the snake with directional images"""
        self.canvas.delete("snake")
        
        for i, (x, y) in enumerate(self.snake):
            center_x = x + self.TILE_SIZE//2
            center_y = y + self.TILE_SIZE//2
            
            if i == 0:  # Head
                if self.images and f"head_{self.direction.lower()}" in self.images:
                    img = self.images[f"head_{self.direction.lower()}"]
                    self.canvas.create_image(center_x, center_y, image=img, tag="snake")
                else:
                    self.rounded_rect(
                        x+2, y+2, x+self.TILE_SIZE-2, y+self.TILE_SIZE-2, 
                        radius=8, fill=self.colors["snake_head_dark"], 
                        outline=self.colors["border"], width=2, tag="snake"
                    )
                    
                    eye_radius = 3
                    if self.direction == "Right":
                        self.canvas.create_oval(
                            x+self.TILE_SIZE-12, y+10, 
                            x+self.TILE_SIZE-6, y+16, 
                            fill=self.colors["eye_white"], outline=self.colors["eye_pupil"], tags="snake"
                        )
                        self.canvas.create_oval(
                            x+self.TILE_SIZE-12, y+self.TILE_SIZE-16, 
                            x+self.TILE_SIZE-6, y+self.TILE_SIZE-10, 
                            fill=self.colors["eye_white"], outline=self.colors["eye_pupil"], tags="snake"
                        )
                    elif self.direction == "Left":
                        self.canvas.create_oval(
                            x+6, y+10, 
                            x+12, y+16, 
                            fill=self.colors["eye_white"], outline=self.colors["eye_pupil"], tags="snake"
                        )
                        self.canvas.create_oval(
                            x+6, y+self.TILE_SIZE-16, 
                            x+12, y+self.TILE_SIZE-10, 
                            fill=self.colors["eye_white"], outline=self.colors["eye_pupil"], tags="snake"
                        )
                    elif self.direction == "Up":
                        self.canvas.create_oval(
                            x+10, y+6, 
                            x+16, y+12, 
                            fill=self.colors["eye_white"], outline=self.colors["eye_pupil"], tags="snake"
                        )
                        self.canvas.create_oval(
                            x+self.TILE_SIZE-16, y+6, 
                            x+self.TILE_SIZE-10, y+12, 
                            fill=self.colors["eye_white"], outline=self.colors["eye_pupil"], tags="snake"
                        )
                    elif self.direction == "Down":
                        self.canvas.create_oval(
                            x+10, y+self.TILE_SIZE-12, 
                            x+16, y+self.TILE_SIZE-6, 
                            fill=self.colors["eye_white"], outline=self.colors["eye_pupil"], tags="snake"
                        )
                        self.canvas.create_oval(
                            x+self.TILE_SIZE-16, y+self.TILE_SIZE-12, 
                            x+self.TILE_SIZE-10, y+self.TILE_SIZE-6, 
                            fill=self.colors["eye_white"], outline=self.colors["eye_pupil"], tags="snake"
                        )
            elif i == len(self.snake)-1:  # Tail
                tail_x, tail_y = self.snake[-1]
                prev_x, prev_y = self.snake[-2]
                
                if tail_x < prev_x:  # Tail pointing left
                    tail_dir = "right"
                elif tail_x > prev_x:  # Tail pointing right
                    tail_dir = "left"
                elif tail_y < prev_y:  # Tail pointing up
                    tail_dir = "down"
                else:  # Tail pointing down
                    tail_dir = "up"
                
                if self.images and f"tail_{tail_dir}" in self.images:
                    img = self.images[f"tail_{tail_dir}"]
                    self.canvas.create_image(center_x, center_y, image=img, tag="snake")
                else:
                    self.canvas.create_rectangle(
                        x, y, x + self.TILE_SIZE, y + self.TILE_SIZE, 
                        fill=self.colors["snake_tail_dark"], 
                        outline=self.colors["border"], 
                        width=2, tag="snake"
                    )
            else:  # Body
                prev_x, prev_y = self.snake[i-1]
                next_x, next_y = self.snake[i+1] if i+1 < len(self.snake) else (x, y)
                
                if (x == prev_x and x == next_x) or (y == prev_y and y == next_y):
                    if x == prev_x:  # Vertical
                        body_type = "ver"
                    else:  # Horizontal
                        body_type = "hor"
                else:
                    body_type = "hor"
                
                if self.images and f"body_{body_type}" in self.images:
                    img = self.images[f"body_{body_type}"]
                    self.canvas.create_image(center_x, center_y, image=img, tag="snake")
                else:
                    self.canvas.create_rectangle(
                        x, y, x + self.TILE_SIZE, y + self.TILE_SIZE, 
                        fill=self.colors["snake_body_dark"], 
                        outline=self.colors["border"], 
                        width=2, tag="snake"
                    )

    def rounded_rect(self, x1, y1, x2, y2, radius=10, **kwargs):
        """Create a rectangle with rounded corners"""
        points = [
            x1+radius, y1,
            x2-radius, y1,
            x2, y1,
            x2, y1+radius,
            x2, y2-radius,
            x2, y2,
            x2-radius, y2,
            x1+radius, y2,
            x1, y2,
            x1, y2-radius,
            x1, y1+radius,
            x1, y1,
            x1+radius, y1
        ]
        return self.canvas.create_polygon(points, **kwargs, smooth=True)

    def show_start_screen(self):
        """Display start screen with difficulty options"""
        self.canvas.delete("all")
        self.create_background()
        self.game_running = False
        self.game_paused = False
        
        # Game title
        self.canvas.create_text(
            self.CANVAS_WIDTH//2, self.CANVAS_HEIGHT//2 - 100,
            text="SNAKE GAME", 
            fill=self.colors["text"], font=("Arial", 24, "bold")
        )
        
        button_y = self.CANVAS_HEIGHT//2 - 30
        difficulties = [
            ("easy", 250),
            ("moderate", 175),
            ("hard", 100)
        ]
        
        for i, (btn_name, speed) in enumerate(difficulties):
            btn_img = self.button_images[btn_name]
            if btn_img:
                btn = tk.Button(self.canvas, image=btn_img, borderwidth=0,
                            command=lambda s=speed: self.set_difficulty_and_start(s))
                btn_window = self.canvas.create_window(
                    self.CANVAS_WIDTH//2, button_y + i*60,
                    window=btn, width=120, height=40
                )

            self.canvas.tag_bind(btn_window, "<Enter>", lambda e, b=btn: b.config(relief=tk.SUNKEN))
            self.canvas.tag_bind(btn_window, "<Leave>", lambda e, b=btn: b.config(relief=tk.FLAT))

            
            self.canvas.tag_bind(btn_img, "<Button-1>", lambda e, s=speed: self.set_difficulty_and_start(s))
            self.canvas.tag_bind(btn_img, "<Button-1>", lambda e, s=speed: self.set_difficulty_and_start(s))


    def set_difficulty_and_start(self, speed):
        """Set game speed and start the game"""
        self.GAME_SPEED = speed
        self.start_game()

    def start_game(self, event=None):
        """Initialize game when button is clicked"""
        self.canvas.delete("all")
        self.create_background()
        
        # Initialize game state
        start_x = 3 * self.TILE_SIZE
        start_y = 3 * self.TILE_SIZE
        self.snake = [
            (start_x, start_y), 
            (start_x - self.TILE_SIZE, start_y), 
            (start_x - 2*self.TILE_SIZE, start_y)
        ]
        self.direction = "Right"
        self.next_direction = "Right"
        self.score = 0
        self.update_score_display()
        self.food = self.create_food()
        self.game_running = True
        self.game_paused = False
        
        self.master.bind("<KeyPress>", self.change_direction)
        self.game_loop()

    def check_collisions(self):
        """Check for wall and self collisions"""
        head_x, head_y = self.snake[0]
        return (head_x < 0 or head_x >= self.CANVAS_WIDTH  or
                head_y < 0 or head_y >= self.CANVAS_HEIGHT or
                (head_x, head_y) in self.snake[1:])
    
    def update_score_display(self):
        """Update the score display in the top border"""
        self.score_label.config(text=f"Score: {self.score}")

    def game_loop(self):
        """Main game loop"""
        if not self.game_running or self.game_paused:
            return

        if self.check_collisions():
            self.game_over()
            return
        
        self.move_snake()
        self.draw_snake()
        self.master.after(self.GAME_SPEED, self.game_loop)

        if self.check_collisions():
            self.game_running = False
            self.canvas.create_rectangle(
                self.CANVAS_WIDTH//2-150, self.CANVAS_HEIGHT//2-50,
                self.CANVAS_WIDTH//2+150, self.CANVAS_HEIGHT//2+50,
                fill=self.colors["popup_bg"], outline=self.colors["border"], width=2
            )
            self.canvas.create_text(
                self.CANVAS_WIDTH//2, self.CANVAS_HEIGHT//2-15,
                text=f"Game Over!",
                fill=self.colors["text"], font=("Arial", 18, "bold")
            )
            self.canvas.create_text(
                self.CANVAS_WIDTH//2, self.CANVAS_HEIGHT//2+15,
                text=f"Score: {self.score}",
                fill=self.colors["text"], font=("Arial", 16)
            )
            
            restart_btn = self.canvas.create_rectangle(
                self.CANVAS_WIDTH//2 - 60, self.CANVAS_HEIGHT//2 + 40,
                self.CANVAS_WIDTH//2 + 60, self.CANVAS_HEIGHT//2 + 70,
                fill=self.colors["button_bg"], outline=self.colors["button_outline"], width=2
            )
            restart_text = self.canvas.create_text(
                self.CANVAS_WIDTH//2, self.CANVAS_HEIGHT//2 + 55,
                text="PLAY AGAIN", 
                fill=self.colors["button_text"], font=("Arial", 14, "bold")
            )
            
            self.canvas.tag_bind(restart_btn, "<Button-1>", self.start_game)
            self.canvas.tag_bind(restart_text, "<Button-1>", self.start_game)
            self.canvas.tag_bind(restart_btn, "<Enter>", lambda e: self.canvas.itemconfig(restart_btn, fill=self.colors["button_hover"]))
            self.canvas.tag_bind(restart_btn, "<Leave>", lambda e: self.canvas.itemconfig(restart_btn, fill=self.colors["button_bg"]))
            return
        
        self.move_snake()
        self.draw_snake()
        self.master.after(self.GAME_SPEED, self.game_loop)  # Changed from 150 to self.GAME_SPEED
    
    def game_over(self):
        """Handle game over state"""
        self.game_running = False
        self.update_high_score()
        
        # Game over popup
        popup = tk.Toplevel(self.master)
        popup.title("Game Over")
        popup.geometry("300x200")
        popup.resizable(False, False)
        popup.transient(self.master)
        popup.grab_set()
        
        # Center the popup
        x = self.master.winfo_x() + (self.CANVAS_WIDTH - 300) // 2
        y = self.master.winfo_y() + (self.CANVAS_HEIGHT - 200) // 2
        popup.geometry(f"300x200+{x}+{y}")
        
        # Popup content
        tk.Label(popup, text="GAME OVER", font=("Arial", 18, "bold")).pack(pady=10)
        tk.Label(popup, text=f"Score: {self.score}", font=("Arial", 14)).pack()
        tk.Label(popup, text=f"High Score: {self.high_score}", font=("Arial", 14)).pack(pady=10)
        
        # Buttons
        btn_frame = tk.Frame(popup)
        btn_frame.pack(pady=10)
        
        if self.button_images["play_again"]:
            again_btn = tk.Button(btn_frame, image=self.button_images["play_again"],
                                borderwidth=0, command=lambda: [popup.destroy(), self.show_start_screen()])
            again_btn.pack(side=tk.LEFT, padx=5)
        
        # Quit button
        if self.button_images["quit"]:
            quit_btn = tk.Button(btn_frame, image=self.button_images["quit"],
                            borderwidth=0, command=self.quit_game)
            quit_btn.pack(side=tk.LEFT, padx=5)

if __name__ == "__main__":
    root = tk.Tk()
    game = SnakeGame(root)
    root.mainloop()