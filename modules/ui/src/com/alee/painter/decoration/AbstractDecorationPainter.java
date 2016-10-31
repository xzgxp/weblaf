/*
 * This file is part of WebLookAndFeel library.
 *
 * WebLookAndFeel library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * WebLookAndFeel library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with WebLookAndFeel library.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.alee.painter.decoration;

import com.alee.extended.behavior.AbstractHoverBehavior;
import com.alee.laf.WebLookAndFeel;
import com.alee.laf.grouping.GroupingLayout;
import com.alee.managers.focus.DefaultFocusTracker;
import com.alee.managers.focus.FocusManager;
import com.alee.managers.focus.FocusTracker;
import com.alee.managers.focus.GlobalFocusListener;
import com.alee.managers.style.Bounds;
import com.alee.managers.style.PainterShapeProvider;
import com.alee.painter.AbstractPainter;
import com.alee.painter.SectionPainter;
import com.alee.utils.*;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import java.awt.*;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.*;
import java.util.List;

/**
 * Abstract decoration painter that can be used by any custom and specific painter.
 *
 * @param <E> component type
 * @param <U> component UI type
 * @param <D> decoration type
 * @author Mikle Garin
 */

public abstract class AbstractDecorationPainter<E extends JComponent, U extends ComponentUI, D extends IDecoration<E, D>>
        extends AbstractPainter<E, U> implements PainterShapeProvider<E>
{
    /**
     * Decoratable states property.
     */
    public static final String DECORATION_STATES_PROPERTY = "decorationStates";

    /**
     * Available decorations.
     */
    protected Decorations<E, D> decorations;

    /**
     * Listeners.
     */
    protected transient FocusTracker focusStateTracker;
    protected transient GlobalFocusListener inFocusedParentTracker;
    protected transient AbstractHoverBehavior<E> hoverStateTracker;
    protected transient HierarchyListener hierarchyTracker;
    protected transient ContainerListener neighboursTracker;

    /**
     * Runtime variables.
     */
    protected transient List<String> states;
    protected transient Map<String, D> stateDecorationCache;
    protected transient Map<String, D> decorationCache;
    protected transient String current;
    protected transient boolean focused = false;
    protected transient boolean inFocusedParent = false;
    protected transient boolean hover = false;
    protected transient Container ancestor;

    @Override
    public void install ( final E c, final U ui )
    {
        super.install ( c, ui );

        // Determining initial decoration state
        this.focused = SwingUtils.hasFocusOwner ( c );
        this.inFocusedParent = updateInFocusedParent ();
        this.states = collectDecorationStates ();

        // Installing listeners
        installFocusListener ();
        installInFocusedParentListener ();
        installHoverListener ();
        installHierarchyListener ();
    }

    @Override
    public void uninstall ( final E c, final U ui )
    {
        // Deactivating decoaration
        deactivateLastDecoration ( c );

        // Uninstalling listeners
        uninstallHierarchyListener ();
        uninstallHoverListener ();
        uninstallFocusListener ();

        // Cleaning up variables
        this.decorationCache = null;
        this.stateDecorationCache = null;
        this.states = null;
        this.focused = false;

        super.uninstall ( c, ui );
    }

    @Override
    protected void propertyChanged ( final String property, final Object oldValue, final Object newValue )
    {
        // Perform basic actions on property changes
        super.propertyChanged ( property, oldValue, newValue );

        // Updating enabled state
        if ( CompareUtils.equals ( property, WebLookAndFeel.ENABLED_PROPERTY ) )
        {
            if ( usesState ( DecorationState.enabled ) || usesState ( DecorationState.disabled ) )
            {
                updateDecorationState ();
            }
        }

        // Updating custom decoration states
        if ( CompareUtils.equals ( property, DECORATION_STATES_PROPERTY ) )
        {
            updateDecorationState ();
        }
    }

    /**
     * Overriden to provide decoration states update instead of simple view updates.
     */
    @Override
    protected void orientationChange ()
    {
        // Saving new orientation
        saveOrientation ();

        // Updating decoration states
        updateDecorationState ();
    }

    /**
     * Installs listener that will perform decoration updates on focus state change.
     */
    protected void installFocusListener ()
    {
        if ( usesState ( DecorationState.focused ) )
        {
            focusStateTracker = new DefaultFocusTracker ( true )
            {
                @Override
                public void focusChanged ( final boolean focused )
                {
                    AbstractDecorationPainter.this.focusChanged ( focused );
                }
            };
            FocusManager.addFocusTracker ( component, focusStateTracker );
        }
    }

    /**
     * Informs about focus state changes.
     *
     * @param focused whether or not component has focus
     */
    protected void focusChanged ( final boolean focused )
    {
        AbstractDecorationPainter.this.focused = focused;
        updateDecorationState ();
    }

    /**
     * Returns whether or not component has focus.
     *
     * @return true if component has focus, false otherwise
     */
    protected boolean isFocused ()
    {
        return focused;
    }

    /**
     * Uninstalls focus listener.
     */
    protected void uninstallFocusListener ()
    {
        if ( focusStateTracker != null )
        {
            FocusManager.removeFocusTracker ( focusStateTracker );
            focusStateTracker = null;
        }
    }

    /**
     * Updates focus listener usage.
     */
    protected void updateFocusListener ()
    {
        if ( usesState ( DecorationState.focused ) )
        {
            installFocusListener ();
        }
        else
        {
            uninstallFocusListener ();
        }
    }

    /**
     * Installs listener that performs decoration updates on focused parent appearance and disappearance.
     */
    protected void installInFocusedParentListener ()
    {
        if ( usesState ( DecorationState.inFocusedParent ) )
        {
            inFocusedParentTracker = new GlobalFocusListener ()
            {
                @Override
                public void focusChanged ( final Component oldFocus, final Component newFocus )
                {
                    globalFocusChanged ( oldFocus, newFocus );
                }
            };
            FocusManager.registerGlobalFocusListener ( inFocusedParentTracker );
        }
    }

    /**
     * Informs about global focus change.
     *
     * @param oldFocus previously focused component
     * @param newFocus currently focused component
     */
    @SuppressWarnings ( "UnusedParameters" )
    protected void globalFocusChanged ( final Component oldFocus, final Component newFocus )
    {
        // Updating {@link #inFocusedParent} mark
        updateInFocusedParent ();
    }

    /**
     * Updates {@link #inFocusedParent} mark.
     * For that we check whether or not any related component gained or lost focus.
     *
     * @return {@code true} if component is placed within focused parent, {@code false} otherwise
     */
    protected boolean updateInFocusedParent ()
    {
        final boolean old = inFocusedParent;
        inFocusedParent = false;
        Container current = component;
        while ( current != null )
        {
            if ( current.isFocusOwner () )
            {
                // Directly in a focused parent
                inFocusedParent = true;
                break;
            }
            else if ( current != component )
            {
                // In a parent that tracks children focus and visually displays it
                // This case is not obvious but really important for correct visual representation of the state
                final ComponentUI ui = LafUtils.getUI ( current );
                if ( ui != null )
                {
                    // todo Replace with proper painter retrieval upon Paintable interface implementation
                    final Object painter = ReflectUtils.getFieldValueSafely ( ui, "painter" );
                    if ( painter != null && painter instanceof AbstractDecorationPainter )
                    {
                        final AbstractDecorationPainter dp = ( AbstractDecorationPainter ) painter;
                        if ( dp.usesState ( DecorationState.focused ) )
                        {
                            inFocusedParent = dp.isFocused ();
                            break;
                        }
                    }
                }
            }
            current = current.getParent ();
        }
        if ( !CompareUtils.equals ( old, inFocusedParent ) )
        {
            updateDecorationState ();
        }
        return inFocusedParent;
    }

    /**
     * Returns whether or not one of this component parents displays focused state.
     * Returns {@code true} if one of parents owns focus directly or indirectly due to one of its children being focused.
     * Indirect focus ownership is only accepted from parents which use {@link DecorationState#focused} decoration state.
     *
     * @return {@code true} if one of this component parents displays focused state, {@code false} otherwise
     */
    protected boolean isInFocusedParent ()
    {
        return inFocusedParent;
    }

    /**
     * Uninstalls global focus listener.
     */
    protected void uninstallInFocusedParentListener ()
    {
        if ( inFocusedParentTracker != null )
        {
            FocusManager.unregisterGlobalFocusListener ( inFocusedParentTracker );
            inFocusedParentTracker = null;
        }
    }

    /**
     * Installs listener that will perform decoration updates on hover state change.
     */
    protected void installHoverListener ()
    {
        if ( hoverStateTracker == null && usesState ( DecorationState.hover ) )
        {
            hoverStateTracker = new AbstractHoverBehavior<E> ( component, false )
            {
                @Override
                public void hoverChanged ( final boolean hover )
                {
                    AbstractDecorationPainter.this.hoverChanged ( hover );
                }
            };
            hoverStateTracker.install ();
        }
    }

    /**
     * Informs about hover state changes.
     *
     * @param hover whether or not mouse is on the component
     */
    protected void hoverChanged ( final boolean hover )
    {
        AbstractDecorationPainter.this.hover = hover;
        updateDecorationState ();
    }

    /**
     * Returns whether or not component is in hover state.
     *
     * @return true if component is in hover state, false otherwise
     */
    protected boolean isHover ()
    {
        return hover;
    }

    /**
     * Uninstalls hover listener.
     */
    protected void uninstallHoverListener ()
    {
        if ( hoverStateTracker != null )
        {
            hoverStateTracker.uninstall ();
            hoverStateTracker = null;
        }
    }

    /**
     * Updates hover listener usage.
     */
    protected void updateHoverListener ()
    {
        if ( usesState ( DecorationState.hover ) )
        {
            installHoverListener ();
        }
        else
        {
            uninstallHoverListener ();
        }
    }

    /**
     * Installs listener that will perform border updates on component hierarchy changes.
     * This is required to properly update decoration borders in case it was moved from or into container with grouping layout.
     * It also tracks neighbour components addition and removal to update this component border accordingly.
     */
    protected void installHierarchyListener ()
    {
        neighboursTracker = new ContainerListener ()
        {
            @Override
            public void componentAdded ( final ContainerEvent e )
            {
                // Updating border when a child was added nearby
                if ( ancestor != null && ancestor.getLayout () instanceof GroupingLayout && e.getChild () != component )
                {
                    updateBorder ();
                }
            }

            @Override
            public void componentRemoved ( final ContainerEvent e )
            {
                // Updating border when a child was removed nearby
                if ( ancestor != null && ancestor.getLayout () instanceof GroupingLayout && e.getChild () != component )
                {
                    updateBorder ();
                }
            }
        };
        hierarchyTracker = new HierarchyListener ()
        {
            @Override
            public void hierarchyChanged ( final HierarchyEvent e )
            {
                AbstractDecorationPainter.this.hierarchyChanged ( e );
            }
        };
        component.addHierarchyListener ( hierarchyTracker );
    }

    /**
     * Informs about hierarchy changes.
     *
     * @param e {@link java.awt.event.HierarchyEvent}
     */
    protected void hierarchyChanged ( final HierarchyEvent e )
    {
        // Listening only for parent change event
        // It will inform us when parent container for this component changes
        // Ancestor listener is not really reliable because it might inform about consequent parent changes
        if ( ( e.getChangeFlags () & HierarchyEvent.PARENT_CHANGED ) == HierarchyEvent.PARENT_CHANGED )
        {
            // If there was a previous container...
            if ( ancestor != null )
            {
                // Stop tracking neighbours
                ancestor.removeContainerListener ( neighboursTracker );
            }

            // Updating ancestor
            ancestor = component.getParent ();

            // If there is a new container...
            if ( ancestor != null )
            {
                // Start tracking neighbours
                ancestor.addContainerListener ( neighboursTracker );

                // Updating border
                updateBorder ();
            }
        }
    }

    /**
     * Uninstalls hierarchy listener.
     */
    protected void uninstallHierarchyListener ()
    {
        component.removeHierarchyListener ( hierarchyTracker );
        hierarchyTracker = null;
        if ( ancestor != null )
        {
            ancestor.removeContainerListener ( neighboursTracker );
            ancestor = null;
        }
        neighboursTracker = null;
    }

    /**
     * Returns section painters used within this painter.
     * Might also return {@code null} in case no section painters are used within this one.
     * This method is used for various internal update mechanisms involving section painters.
     *
     * @return section painters used within this painter
     */
    protected List<SectionPainter<E, U>> getSectionPainters ()
    {
        return null;
    }

    /**
     * Returns section painters list in a most optimal way.
     * Utility method for usage inside of classed extending this one.
     *
     * @param sections section painters, some or all of them can be {@code null}
     * @return section painters list in a most optimal way
     */
    protected final List<SectionPainter<E, U>> asList ( final SectionPainter<E, U>... sections )
    {
        ArrayList<SectionPainter<E, U>> list = null;
        if ( sections != null )
        {
            for ( final SectionPainter<E, U> section : sections )
            {
                if ( section != null )
                {
                    if ( list == null )
                    {
                        list = new ArrayList<SectionPainter<E, U>> ( sections.length );
                    }
                    list.add ( section );
                }
            }
        }
        return list;
    }

    /**
     * Returns whether or not component is in enabled state.
     *
     * @return {@code true} if component is in enabled state, {@code false} otherwise
     */
    protected boolean isEnabled ()
    {
        return component != null && component.isEnabled ();
    }

    /**
     * Returns properly sorted current component decoration states.
     *
     * @return properly sorted current component decoration states
     */
    protected final List<String> collectDecorationStates ()
    {
        // Retrieving current decoration states
        final List<String> states = getDecorationStates ();

        // Adding custom UI decoration states
        if ( ui instanceof Stateful )
        {
            final List<String> uiStates = ( ( Stateful ) ui ).getStates ();
            if ( !CollectionUtils.isEmpty ( uiStates ) )
            {
                states.addAll ( uiStates );
            }
        }

        // Adding custom component decoration states
        if ( component instanceof Stateful )
        {
            final List<String> componentStates = ( ( Stateful ) component ).getStates ();
            if ( !CollectionUtils.isEmpty ( componentStates ) )
            {
                states.addAll ( componentStates );
            }
        }

        // Sorting states to always keep the same order
        Collections.sort ( states );

        return states;
    }

    /**
     * Returns current component decoration states.
     *
     * @return current component decoration states
     */
    protected List<String> getDecorationStates ()
    {
        final List<String> states = new ArrayList<String> ( 12 );
        states.add ( SystemUtils.getShortOsName () );
        states.add ( isEnabled () ? DecorationState.enabled : DecorationState.disabled );
        states.add ( ltr ? DecorationState.leftToRight : DecorationState.rightToLeft );
        if ( isFocused () )
        {
            states.add ( DecorationState.focused );
        }
        if ( isInFocusedParent () )
        {
            states.add ( DecorationState.inFocusedParent );
        }
        if ( isHover () )
        {
            states.add ( DecorationState.hover );
        }
        return states;
    }

    /**
     * Returns whether component has decoration associated with specified state.
     *
     * @param state decoration state
     * @return {@code true} if component has decoration associated with specified state, {@code false} otherwise
     */
    protected boolean usesState ( final String state )
    {
        // Checking whether or not this painter uses this decoration state
        boolean usesState = usesState ( decorations, state );

        // Checking whether or not section painters used by this painter use it
        if ( !usesState )
        {
            final List<SectionPainter<E, U>> sectionPainters = getSectionPainters ();
            if ( !CollectionUtils.isEmpty ( sectionPainters ) )
            {
                for ( final SectionPainter<E, U> section : sectionPainters )
                {
                    if ( section instanceof AbstractDecorationPainter )
                    {
                        if ( ( ( AbstractDecorationPainter ) section ).usesState ( state ) )
                        {
                            usesState = true;
                            break;
                        }
                    }
                }
            }
        }

        return usesState;
    }

    /**
     * Returns whether specified decorations are associated with specified state.
     *
     * @param decorations decorations
     * @param state       decoration state
     * @return {@code true} if specified decorations are associated with specified state, {@code false} otherwise
     */
    protected final boolean usesState ( final Decorations<E, D> decorations, final String state )
    {
        if ( decorations != null && decorations.size () > 0 )
        {
            for ( final D decoration : decorations )
            {
                if ( decoration.usesState ( state ) )
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns decorations for the specified states.
     *
     * @param forStates decoration states to retrieve decoration for
     * @return decorations for the specified states
     */
    protected List<D> getDecorations ( final List<String> forStates )
    {
        if ( decorations != null && decorations.size () > 0 )
        {
            final List<D> d = new ArrayList<D> ( 1 );
            for ( final D decoration : decorations )
            {
                if ( decoration.isApplicableTo ( forStates ) )
                {
                    d.add ( decoration );
                }
            }
            return d;
        }
        else
        {
            return null;
        }
    }

    /**
     * Returns decoration matching current states.
     * Decorations returned here are cached copies of the data presented in skins.
     * This was made to avoid corrupting inital data and to increase the decoration retrieval speed.
     *
     * @return decoration matching current states
     */
    protected D getDecoration ()
    {
        // Optimization for painter without decorations
        if ( decorations != null && decorations.size () > 0 )
        {
            // Decoration key
            // States are properly sorted, so their order is always the same
            final String previous = this.current;
            current = TextUtils.listToString ( states, "," );

            // Creating decoration caches
            if ( stateDecorationCache == null )
            {
                // State decorations cache
                // Entry: [ component state -> built decoration reference ]
                // It is used for fastest possible access to component state decorations
                stateDecorationCache = new HashMap<String, D> ( decorations.size () );

                // Decoration combinations cache
                // Entry: [ decorations combination key -> built decoration reference ]
                // It is used to avoid excessive memory usage by duplicate decoration combinations for each specific state
                decorationCache = new HashMap<String, D> ( decorations.size () );
            }

            // Resolving state decoration if it is not yet cached
            if ( !stateDecorationCache.containsKey ( current ) )
            {
                // Retrieving all decorations fitting current states
                final List<D> decorations = getDecorations ( states );

                // Retrieving unique key for decorations combination
                final String decorationsKey = getDecorationsKey ( decorations );

                // Retrieving existing decoration or building a new one
                final D decoration;
                if ( decorationCache.containsKey ( decorationsKey ) )
                {
                    // Retrieving decoration from existing built decorations cache
                    decoration = decorationCache.get ( decorationsKey );
                }
                else
                {
                    // Building single decoration from a set
                    if ( CollectionUtils.isEmpty ( decorations ) )
                    {
                        // No decoration for the states available
                        decoration = null;
                    }
                    else if ( decorations.size () == 1 )
                    {
                        // Single existing decoration for the states
                        decoration = MergeUtils.clone ( decorations.get ( 0 ) );
                    }
                    else
                    {
                        // Filter out possible decorations of different type
                        // We always use type of the last one available since it has higher priority
                        final Class<? extends IDecoration> type = decorations.get ( decorations.size () - 1 ).getClass ();
                        final Iterator<D> iterator = decorations.iterator ();
                        while ( iterator.hasNext () )
                        {
                            final D d = iterator.next ();
                            if ( d.getClass () != type )
                            {
                                iterator.remove ();
                            }
                        }

                        // Merging multiple decorations together
                        decoration = MergeUtils.clone ( decorations.get ( 0 ) );
                        for ( int i = 1; i < decorations.size (); i++ )
                        {
                            decoration.merge ( decorations.get ( i ) );
                        }
                    }

                    // Updating built decoration settings
                    if ( decoration != null )
                    {
                        // Updating section mark
                        // This is done for each cached decoration once as it doesn't change
                        decoration.setSection ( isSectionPainter () );

                        // Filling decoration states with all current states
                        // This is required so that decoration correctly stores all states, not just ones it uses
                        decoration.updateStates ( CollectionUtils.copy ( states ) );
                    }

                    // Caching built decoration
                    decorationCache.put ( decorationsKey, decoration );
                }

                // Caching resulting decoration under the state key
                stateDecorationCache.put ( current, decoration );
            }

            // Performing decoration activation and deactivation if needed
            if ( previous == null && current == null )
            {
                // Activating initial decoration
                final D initialDecoration = stateDecorationCache.get ( current );
                if ( initialDecoration != null )
                {
                    initialDecoration.activate ( component );
                }
            }
            else if ( !CompareUtils.equals ( previous, current ) )
            {
                // Checking that decoration was actually changed
                final D previousDecoration = stateDecorationCache.get ( previous );
                final D currentDecoration = stateDecorationCache.get ( current );
                if ( previousDecoration != currentDecoration )
                {
                    // Deactivating previous decoration
                    if ( previousDecoration != null )
                    {
                        previousDecoration.deactivate ( component );
                    }
                    // Activating current decoration
                    if ( currentDecoration != null )
                    {
                        currentDecoration.activate ( component );
                    }
                }
            }

            // Returning existing decoration
            return stateDecorationCache.get ( current );
        }
        else
        {
            // No decorations added
            return null;
        }
    }

    /**
     * Returns unique decorations combination key.
     *
     * @param decorations decorations to retrieve unique combination key for
     * @return unique decorations combination key
     */
    protected String getDecorationsKey ( final List<D> decorations )
    {
        final StringBuilder key = new StringBuilder ( 15 * decorations.size () );
        for ( final D decoration : decorations )
        {
            if ( key.length () > 0 )
            {
                key.append ( ";" );
            }
            key.append ( decoration.getId () );
        }
        return key.toString ();
    }

    /**
     * Performs deactivation of the recently used decoration.
     *
     * @param c painted component
     */
    protected void deactivateLastDecoration ( final E c )
    {
        final D decoration = getDecoration ();
        if ( decoration != null )
        {
            decoration.deactivate ( c );
        }
    }

    /**
     * Performs current decoration state update.
     */
    protected void updateDecorationState ()
    {
        final List<String> states = collectDecorationStates ();
        if ( !CollectionUtils.equals ( this.states, states ) )
        {
            // Saving new decoration states
            this.states = states;

            // Updating section painters decoration states
            // This is required to provide state changes into section painters used within this painter
            final List<SectionPainter<E, U>> sectionPainters = getSectionPainters ();
            if ( !CollectionUtils.isEmpty ( sectionPainters ) )
            {
                for ( final SectionPainter<E, U> section : sectionPainters )
                {
                    if ( section instanceof AbstractDecorationPainter )
                    {
                        ( ( AbstractDecorationPainter ) section ).updateDecorationState ();
                    }
                }
            }

            // Updating component visual state
            revalidate ();
            repaint ();
        }
    }

    @Override
    public Insets getBorders ()
    {
        final IDecoration decoration = getDecoration ();
        return decoration != null ? decoration.getBorderInsets ( component ) : null;
    }

    @Override
    public Shape provideShape ( final E component, final Rectangle bounds )
    {
        final IDecoration decoration = getDecoration ();
        return decoration != null ? decoration.provideShape ( component, bounds ) : bounds;
    }

    @Override
    public Boolean isOpaque ()
    {
        // Calculates opacity state based on provided decorations
        // In case there is an active visible decoration component should not be opaque
        // This might be changed in future and moved into decoration to provide specific setting
        final IDecoration decoration = getDecoration ();
        return decoration != null ? isOpaqueDecorated () : isOpaqueUndecorated ();
    }

    /**
     * Returns opacity state for decorated component.
     * This is separated from base opacity state method to allow deep customization.
     *
     * @return opacity state for decorated component
     */
    protected Boolean isOpaqueDecorated ()
    {
        // Returns {@code false} to make component non-opaque when decoration is available
        // This is convenient because almost any custom decoration might have transparent spots in it
        // And if it has any it cannot be opaque or it will cause major painting glitches
        return false;
    }

    /**
     * Returns opacity state for undecorated component.
     * This is separated from base opacity state method to allow deep customization.
     *
     * @return opacity state for undecorated component
     */
    protected Boolean isOpaqueUndecorated ()
    {
        // Returns {@code null} to disable automatic opacity changes by default
        // You may still provide a non-null opacity in your own painter implementations
        return null;
    }

    @Override
    public int getBaseline ( final E c, final U ui, final int width, final int height )
    {
        final D decoration = getDecoration ();
        return decoration != null ? decoration.getBaseline ( c, width, height ) : super.getBaseline ( c, ui, width, height );
    }

    @Override
    public void paint ( final Graphics2D g2d, final Rectangle bounds, final E c, final U ui )
    {
        final Rectangle b = adjustBounds ( bounds );

        // Paint simple background if opaque
        // Otherwise component might cause various visual glitches
        if ( isPlainBackgroundPaintAllowed ( c ) )
        {
            g2d.setPaint ( c.getBackground () );
            g2d.fill ( isSectionPainter () ? b : Bounds.component.of ( c, b ) );
        }

        // Painting current decoration state
        final D decoration = getDecoration ();
        if ( isDecorationPaintAllowed ( decoration ) )
        {
            decoration.paint ( g2d, isSectionPainter () ? Bounds.margin.of ( c, decoration, b ) : Bounds.margin.of ( c, b ), c );
        }

        // Painting content
        // todo This is a temporary method and should be removed upon complete styling implementation
        // todo if ( decoration == null || !decoration.hasContent () )
        paintContent ( g2d, b, c, ui );
    }

    /**
     * Paints content decorated by this painter.
     * todo This method should eventually be removed since all content will be painted by IContent implementations
     *
     * @param g2d    graphics context
     * @param bounds painting bounds
     * @param c      painted component
     * @param ui     painted component UI
     */
    protected void paintContent ( final Graphics2D g2d, final Rectangle bounds, final E c, final U ui )
    {
        // No content by default
    }

    /**
     * Returns adjusted painting bounds.
     * todo This should be replaced with proper bounds provided from outside
     *
     * @param bounds painting bounds to adjust
     * @return adjusted painting bounds
     */
    protected Rectangle adjustBounds ( final Rectangle bounds )
    {
        return bounds;
    }

    /**
     * Returns whether or not painting plain component background is allowed.
     * Moved into separated method for convenient background painting blocking using additional conditions.
     * <p>
     * By default this condition is limited to component being opaque.
     * When component is opaque we must fill every single pixel in its bounds with something to avoid issues.
     *
     * @param c component to paint background for
     * @return {@code true} if painting plain component background is allowed, {@code false} otherwise
     */
    protected boolean isPlainBackgroundPaintAllowed ( final E c )
    {
        return c.isOpaque ();
    }

    /**
     * Returns whether or not painting specified decoration is allowed.
     * Moved into separated method for convenient decorationg painting blocking using additional conditions.
     * <p>
     * By default this condition is limited to decoration existance and visibility.
     *
     * @param decoration decoration to be painted
     * @return {@code true} if painting specified decoration is allowed, {@code false} otherwise
     */
    protected boolean isDecorationPaintAllowed ( final D decoration )
    {
        return decoration != null;
    }

    @Override
    public Dimension getPreferredSize ()
    {
        final Dimension ps = super.getPreferredSize ();
        final D d = getDecoration ();
        return d != null ? SwingUtils.max ( d.getPreferredSize ( component ), ps ) : ps;
    }
}