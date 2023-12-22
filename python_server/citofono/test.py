import curses

def main(stdscr):
    # Impostazione del terminale
    curses.curs_set(0)
    stdscr.clear()
    stdscr.refresh()

    # Creazione di una finestra
    height, width = stdscr.getmaxyx()
    win = curses.newwin(10, 40, height // 2 - 5, width // 2 - 20)

    # Stampa di un messaggio
    win.addstr(1, 1, "Raspberry Pi Configuration")
    win.addstr(3, 1, "Select an option:")
    win.refresh()

    # Creazione di una casella combinata
    options = ["Option 1", "Option 2", "Option 3"]
    current_option = 0

    while True:
        for i, option in enumerate(options):
            x = 3 + i
            y = 1
            if i == current_option:
                win.addstr(x, y, option, curses.A_REVERSE)
            else:
                win.addstr(x, y, option)

        key = win.getch()

        if key == curses.KEY_UP and current_option > 0:
            current_option -= 1
        elif key == curses.KEY_DOWN and current_option < len(options) - 1:
            current_option += 1
        elif key == 10:  # Enter key
            win.addstr(8, 1, f"Selected Option: {options[current_option]}")
            win.refresh()

        win.refresh()

if __name__ == "__main__":
    curses.wrapper(main)
