import java.io.*;
import java.util.*;

public class NFAToDFA {

    static class Automata{
        public ArrayList<String> alphabet = new ArrayList<>();
        public ArrayList<String> states = new ArrayList<>();
        public String start_state;
        public ArrayList<String> final_state = new ArrayList<>();
        public ArrayList<String> transitions = new ArrayList<>();
    }


    static class State{
        String state;
        private Map<String, String> transition = new HashMap<>();

        State(String state){
            this.state = state;
        }
        public void add_transition(String c, String s){
            if(transition.containsKey(c)){
                String new_transition = transition.get(c);
                new_transition += s;
                new_transition = get_actual_name(new_transition);
                transition.put(c, new_transition);
            }
            else{
                transition.put(c, s);
            }
        }
        public String get_transition(String c){return transition.get(c);}
    }


    public static ArrayList<String> read_file(String file_name){
        ArrayList<String> lines = new ArrayList<>();
        try{
            Scanner scanner = new Scanner(new File(file_name));
            while (scanner.hasNextLine()){
                lines.add(scanner.nextLine());
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return lines;
    }


    public static void write_to_file(String filename, String str){
        try {
            FileWriter myWriter = new FileWriter(filename);
            myWriter.write(str);
            myWriter.close();
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }


    public static String get_actual_name(String str){
        char[] chars = str.toCharArray();
        Set<Character> tree_set= new TreeSet<>();
        for(char c : chars){
            tree_set.add(c);
        }
        StringBuilder sb = new StringBuilder();
        for(Character c: tree_set){
            sb.append(c);
        }
        return sb.toString();
    }

    public static Automata parse_lines(ArrayList<String> lines){
        Automata a = new Automata();
        int degree = 0;
        for(String line: lines){
            if(line.equals("ALPHABET")){
                degree++;
            }
            else if(degree == 1){
                if (line.equals("STATES")){
                    degree++;
                }
                else{
                    a.alphabet.add(Character.toString(line.charAt(0)));
                }
            }
            else if (degree == 2){
                if (line.equals("START")){
                    degree++;
                }
                else{
                    a.states.add(Character.toString(line.charAt(0)));
                }
            }
            else if (degree == 3){
                if (line.equals("FINAL")){
                    degree++;
                }
                else{
                    a.start_state = Character.toString(line.charAt(0));
                }
            }
            else if (degree == 4){
                if (line.equals("TRANSITIONS")){
                    degree++;
                }
                else{
                    a.final_state.add(Character.toString(line.charAt(0)));
                }
            }
            else if (degree == 5){
                if (line.equals("END")){
                    degree++;
                }
                else{
                    a.transitions.add(line);
                }
            }
            else{
                break;
            }
        }
        return a;
    }

    public static int state_index(ArrayList<State> states, String name){
        for(State s: states){
            if (s.state.equals(name)) {
                return states.indexOf(s);
            }
        }
        return -1;
    }

    public static void main(String[] args) {
        boolean dead = false;
        ArrayList<String> lines = read_file("NFA2.txt");
        Automata nfa = parse_lines(lines);
        ArrayList<State> nfa_states = new ArrayList<>();
        ArrayList<State> dfa_states = new ArrayList<>();

        for(String a: nfa.states){
            State st = new State(a);
            st.state = a;
            nfa_states.add(st);
        }
        for(String t: nfa.transitions){
            String[] sp = t.split(" ");
            String from = sp[0];
            String with = sp[1];
            String to = sp[2];
            for(State state: nfa_states){
                if (state.state.equals(from)){
                    state.add_transition(with, to);
                }
            }
        }

        // ALGORITHM START
        Automata dfa = new Automata();
        //State dead = new State('X');
        State start = new State(nfa.start_state);
        //dfa_states.add(dead); // dead state
        dfa_states.add(start);

        for(int i = 0 ; i < dfa_states.size() ; i++){
            String name = dfa_states.get(i).state;
            if(state_index(nfa_states,name) == -1){ // no state in nfa
                State mergestate = new State(name);
                for(Character c : name.toCharArray()){
                    String st = Character.toString(c);
                    int st_index = state_index(nfa_states, st);
                    Map<String, String> m = nfa_states.get(st_index).transition;
                    m.forEach(mergestate::add_transition);
                }
                nfa_states.add(mergestate);
            }

            for(State nfastate: nfa_states){
                if(nfastate.state.equals(name)){
                    for(String alp: nfa.alphabet){
                        String tr_state = nfastate.get_transition(alp);
                        if(tr_state != null){
                            dfa_states.get(i).add_transition(alp, tr_state);
                            if(state_index(dfa_states, tr_state) == -1){
                                State newstate = new State(tr_state);
                                dfa_states.add(newstate);
                            }
                            //System.out.printf("Added from %s with %s to %s%n",name, alp,tr_state);
                            //dfa.transitions.add(name + " " + alp + " " + tr_state);
                        }else{
                            dfa_states.get(i).add_transition(alp, "DEAD"); // dead state
                            dead = true;
                            //System.out.printf("Added from %s with %s to X%n",name, alp);
                            //dfa.transitions.add(name + " " + alp + " " + tr_state);
                        }
                    }
                }
            }

        }

        // write elements to dfa than to a file
        dfa.start_state = nfa.start_state;
        for(State s: dfa_states){
            String name = s.state;
            dfa.states.add(name);
            Map<String, String> m = s.transition;
            m.forEach((k, v) -> dfa.transitions.add(name + " " + k + " " + v));
        }
        for(String f: nfa.final_state){
            for(String dfa_state: dfa.states){
                if(dfa_state.contains(f)){
                    dfa.final_state.add(dfa_state);
                }
            }
        }
        dfa.alphabet = nfa.alphabet;
        if(dead){
            dfa.states.add("dead");
            for(String alp: dfa.alphabet){
                dfa.transitions.add("dead " + alp + " dead");
            }
        }

        // write to file
        StringBuilder str = new StringBuilder();
        str.append("ALPHABET\n");
        for (String s: dfa.alphabet){
            str.append(s).append("\n");
        }
        str.append("STATES\n");;
        for (String s: dfa.states){
            str.append(s).append("\n");
        }
        str.append("START\n");;
        str.append(dfa.start_state).append("\n");;
        str.append("FINAL\n");
        for (String s: dfa.final_state){
            str.append(s).append("\n");
        }
        str.append("TRANSITIONS\n");
        for (String s: dfa.transitions){
            str.append(s).append("\n");
        }
        str.append("END\n");

        write_to_file("DFA.txt", str.toString());
    }
}

















