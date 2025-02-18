# Party Health Status
Visual representation of your party members hitpoints
  
https://github.com/user-attachments/assets/86e9dfa3-2e5a-4a7c-b66f-be26bfd44627
  
# Visual Overlay
  
  ![PartyHealthVisualOverlay](https://github.com/user-attachments/assets/acfec78d-4b8f-4cbc-891a-420e9c004992)
  
* Optional toggles to not draw the overlay.
  
  ![PartyHealthVisualToggles](https://github.com/user-attachments/assets/c0ab34ce-bec4-4677-aa67-9202f70f4b05)
  
* Lists of specific players to either show or hide from drawing.<br/>
  - Player names are separated by commas i.e | Joe,Billy,Bob
  - If no players are specified ***ALL*** party members will be drawn.
  
  ![PartyHealthVisualLists](https://github.com/user-attachments/assets/21e5b1cf-5f09-4675-9645-0a1c5845c7e3)
  
<a id="Healthy"></a>
* Optional offset that determines what hitpoints are considered to be healthy.
  
  ![PartyHealthHealthy](https://github.com/user-attachments/assets/060a44e0-8070-4dde-b275-74844d6edada)
  
  - For certain visuals the player must be missing 1HP, this value can be offset with this setting.<br/>
    i.e with an offset of 9, the player won't render visuals until they're missing more than 9 HP.
  
* Toggles a rounded hitbox for the given players and specifies the opacity/intensity of the color if rendered.
  
  ![PartyHealthHull](https://github.com/user-attachments/assets/8cbca490-8d55-4170-b27c-c98ac458943c)
  
* Recolors menu options when the heal other spell is selected.
  
  ![PartyHealthHealOther](https://github.com/user-attachments/assets/ceb256c7-de1c-4775-9d4d-e9bb3ab7521d)
  
  - The name of the player is colored based on their current HP, in the same manner that the HP overlay is configured.
  - Instead of displaying combat level, the current HP is displayed within the parenthesis.
  - Healthy players will not display an HP and instead have the entire menu option greyed out.
  
* The calculation used to determine the varying hitpoints colors.
  
  ![PartyHealthColortype](https://github.com/user-attachments/assets/6adae959-c5dc-4a83-81ab-943ba5900b77)
  
  - Color thresholds
    * You specify the Low and Medium value.<br/>
    * Low Color represents HP equal to or less than the specified ***LOW*** value.<br/>
    * Medium Color represents HP equal to or less than the specified ***MEDIUM*** value.<br/>
    * High Color represents HP greater than ***MEDIUM*** value and under the calculated <a href="#Healthy" title="See Healthy Offset config mentioned above.">Healthy</a> value.
  
  - Lerp 3d
    * Linearly interpolates the color between 3 points.
    * Red being the lowest (0%), Orange being the middle (50%) and Green being the highest (100%).
  
  - Lerp 2d
    * Linearly interpolates the color between 2 points.
    * Red being the lowest (0%) Green being the highest (100%).
    * Introduces a Hitpoints Minimum, this is used to dictate when the value is fully red.
  
  - Static
    * The color will always be the specified <a href="#Healthy" title="See Healthy Color config mentioned above.">Healthy</a> Color.
  
# Text Overlay
  
  ![PartyHealthTextOverlay](https://github.com/user-attachments/assets/aaa89858-a7e0-4440-859f-04b6338e1181)
  
  * Conditional text rendering
  
    ![PartyHealthTextRenders](https://github.com/user-attachments/assets/692699c8-282e-4645-af47-d762b0cf5768)
  
    - Configures when to render the given name or HP of a player.
    - The ***When Missing HP*** option uses the <a href="#Healthy" title="See Healthy Offset config mentioned above.">Healthy Offset</a> and renders if the player is missing more than that specified value in HP.
  
  * Postfix
  
    ![PartyHealthTextPostfix](https://github.com/user-attachments/assets/4d701739-0894-4191-85ac-558a4479ac2d)
  
    - Draw percent appends a % symbol to the player HP | 77%
    - Draw parentheses surrounds the player HP with parentheses | (77)
    - Enabling both would result in | (77%)
  
  * Standard Offsets
  
    ![PartyHealthTextOffSetStandard](https://github.com/user-attachments/assets/699005c0-7407-4a66-8871-fdd0f85cc3e6)
  
    - Offsets the player name and HP on the specified Axis.
  
  * Stack Offset
  
    ![PartyHealthTextOffSetStack](https://github.com/user-attachments/assets/369c7d1b-a944-4781-a4c9-b4a5814d1d6a)
  
    - When multiple players are on the same tile, their displayed text is stacked vertically.<br/>
      This offset specifies the vertical spacing between each players respective text.
  
  * Font
  
    ![PartyHealthTextFont](https://github.com/user-attachments/assets/80601e84-68bc-42e8-9eca-4ad41a11fd54)
  
    - Specifies the size and thickness of the text characters.
