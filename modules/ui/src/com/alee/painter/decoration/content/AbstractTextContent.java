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

package com.alee.painter.decoration.content;

import com.alee.managers.style.StyleException;
import com.alee.painter.decoration.DecorationException;
import com.alee.painter.decoration.IDecoration;
import com.alee.utils.*;
import com.alee.utils.swing.BasicHTML;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import javax.swing.*;
import javax.swing.text.View;
import java.awt.*;
import java.util.Map;

/**
 * Abstract implementation of simple text content.
 *
 * @param <E> component type
 * @param <D> decoration type
 * @param <I> content type
 * @author Mikle Garin
 * @author Alexandr Zernov
 */

@SuppressWarnings ( "UnusedParameters" )
public abstract class AbstractTextContent<E extends JComponent, D extends IDecoration<E, D>, I extends AbstractTextContent<E, D, I>>
        extends AbstractContent<E, D, I> implements SwingConstants
{
    /**
     * Preferred text antialias option.
     */
    @XStreamAsAttribute
    protected TextRasterization rasterization;

    /**
     * Text foreground color.
     */
    @XStreamAsAttribute
    protected Color color;

    /**
     * Horizontal text alignment.
     */
    @XStreamAsAttribute
    protected Integer halign;

    /**
     * Vertical text alignment.
     */
    @XStreamAsAttribute
    protected Integer valign;

    /**
     * Whether or not text should be truncated when it gets outside of the available bounds.
     */
    @XStreamAsAttribute
    protected Boolean truncate;

    /**
     * Whether or not should paint text shadow.
     */
    @XStreamAsAttribute
    protected Boolean shadow;

    /**
     * Text shadow color.
     */
    @XStreamAsAttribute
    protected Color shadowColor;

    /**
     * Text shadow size.
     */
    @XStreamAsAttribute
    protected Integer shadowSize;

    /**
     * Cached HTML {@link View} settings.
     * This is runtime-only field that should not be serialized.
     * It is also generated automatically on demand if missing.
     *
     * @see #getHtml(javax.swing.JComponent, com.alee.painter.decoration.IDecoration)
     * @see #cleanupHtml(javax.swing.JComponent, com.alee.painter.decoration.IDecoration)
     */
    protected transient String htmlSettings;

    /**
     * Cached HTML {@link View}.
     * This is runtime-only field that should not be serialized.
     * It is also generated automatically on demand if missing.
     *
     * @see #getHtml(javax.swing.JComponent, com.alee.painter.decoration.IDecoration)
     * @see #cleanupHtml(javax.swing.JComponent, com.alee.painter.decoration.IDecoration)
     */
    protected transient View htmlView;

    @Override
    public String getId ()
    {
        return id != null ? id : "text";
    }

    @Override
    public boolean isEmpty ( final E c, final D d )
    {
        return TextUtils.isEmpty ( getText ( c, d ) );
    }

    @Override
    public void deactivate ( final E c, final D d )
    {
        // Performing default actions
        super.deactivate ( c, d );

        // Cleaning up HTML caches
        cleanupHtml ( c, d );
    }

    /**
     * Returns preferred rasterization option.
     *
     * @return preferred rasterization option
     */
    public TextRasterization getRasterization ()
    {
        return rasterization != null ? rasterization : TextRasterization.subpixel;
    }

    /**
     * Returns text font.
     *
     * @param c painted component
     * @param d painted decoration state
     * @return text font
     */
    protected Font getFont ( final E c, final D d )
    {
        return c.getFont ();
    }

    /**
     * Returns text font metrics.
     *
     * @param c painted component
     * @param d painted decoration state
     * @return text font metrics
     */
    protected FontMetrics getFontMetrics ( final E c, final D d )
    {
        return c.getFontMetrics ( getFont ( c, d ) );
    }

    /**
     * Returns text foreground color.
     *
     * @param c painted component
     * @param d painted decoration state
     * @return text foreground color
     */
    protected Color getColor ( final E c, final D d )
    {
        // This {@link javax.swing.plaf.UIResource} check allows us to ignore such colors in favor of style ones
        // But this will not ignore any normal color set from the code as this component foreground
        return color != null && SwingUtils.isUIResource ( c.getForeground () ) ? color : c.getForeground ();
    }

    /**
     * Returns text horizontal alignment.
     *
     * @param c painted component
     * @param d painted decoration state
     * @return text horizontal alignment
     */
    protected int getHorizontalAlignment ( final E c, final D d )
    {
        final int alignment = halign != null ? halign : LEADING;
        if ( alignment == LEADING )
        {
            return isLeftToRight ( c, d ) ? LEFT : RIGHT;
        }
        else if ( alignment == TRAILING )
        {
            return isLeftToRight ( c, d ) ? RIGHT : LEFT;
        }
        else
        {
            return alignment;
        }
    }

    /**
     * Returns text horizontal alignment adjusted according to the component orientation.
     *
     * @param c painted component
     * @param d painted decoration state
     * @return text horizontal alignment
     */
    protected int getAdjustedHorizontalAlignment ( final E c, final D d )
    {
        final int alignment = getHorizontalAlignment ( c, d );
        switch ( alignment )
        {
            case LEADING:
                return isLeftToRight ( c, d ) ? LEFT : RIGHT;

            case TRAILING:
                return isLeftToRight ( c, d ) ? RIGHT : LEFT;

            default:
                return alignment;
        }
    }

    /**
     * Returns text vertical alignment.
     *
     * @param c painted component
     * @param d painted decoration state
     * @return text vertical alignment
     */
    protected int getVerticalAlignment ( final E c, final D d )
    {
        return valign != null ? valign : CENTER;
    }

    /**
     * Returns whether or not text should be truncated when it gets outside of the available bounds.
     *
     * @param c painted component
     * @param d painted decoration state
     * @return true if text should be truncated when it gets outside of the available bounds, false otherwise
     */
    protected boolean isTruncate ( final E c, final D d )
    {
        return truncate == null || truncate;
    }

    /**
     * Returns whether or not text shadow should be painted.
     *
     * @param c painted component
     * @param d painted decoration state
     * @return true if text shadow should be painted, false otherwise
     */
    protected boolean isShadow ( final E c, final D d )
    {
        return shadow != null && shadow;
    }

    /**
     * Returns shadow color.
     *
     * @param c painted component
     * @param d painted decoration state
     * @return shadow color
     */
    protected Color getShadowColor ( final E c, final D d )
    {
        if ( shadowColor != null )
        {
            return shadowColor;
        }
        throw new StyleException ( "Shadow color must be specified" );
    }

    /**
     * Returns shadow size.
     *
     * @param c painted component
     * @param d painted decoration state
     * @return shadow size
     */
    protected int getShadowSize ( final E c, final D d )
    {
        if ( shadowSize != null )
        {
            return shadowSize;
        }
        throw new StyleException ( "Shadow size must be specified" );
    }

    /**
     * Returns whether or not text contains HTML.
     *
     * @param c painted component
     * @param d painted decoration state
     * @return true if text contains HTML, false otherwise
     */
    protected boolean isHtmlText ( final E c, final D d )
    {
        // Determining whether or not text contains HTML
        final String text = getText ( c, d );
        final boolean html = BasicHTML.isHTMLString ( c, text );

        // Cleaning up HTML caches
        if ( !html )
        {
            cleanupHtml ( c, d );
        }

        return html;
    }

    /**
     * Returns HTML text view to be painted.
     *
     * @param c painted component
     * @param d painted decoration state
     * @return HTML text view to be painted
     */
    protected View getHtml ( final E c, final D d )
    {
        // HTML content settings
        final String text = getText ( c, d );
        final Font defaultFont = getFont ( c, d );
        final Color foreground = getColor ( c, d );

        // HTML view settings
        final String settings = text + ";" + defaultFont + ";" + foreground;

        // Updating HTML view if needed
        if ( htmlView == null || !CompareUtils.equals ( htmlSettings, settings ) )
        {
            htmlSettings = settings;
            htmlView = BasicHTML.createHTMLView ( c, text, defaultFont, foreground );
        }

        // Return cached HTML view
        return htmlView;
    }

    /**
     * Cleans up HTML text view caches.
     *
     * @param c painted component
     * @param d painted decoration state
     */
    protected void cleanupHtml ( final E c, final D d )
    {
        htmlSettings = null;
        htmlView = null;
    }

    /**
     * Returns text to be painted.
     *
     * @param c painted component
     * @param d painted decoration state
     * @return text to be painted
     */
    protected abstract String getText ( E c, D d );

    /**
     * Returns mnemonic index or {@code -1} if it shouldn't be displayed.
     *
     * @param c painted component
     * @param d painted decoration state
     * @return mnemonic index or {@code -1} if it shouldn't be displayed
     */
    protected abstract int getMnemonicIndex ( E c, D d );

    @Override
    protected void paintContent ( final Graphics2D g2d, final Rectangle bounds, final E c, final D d )
    {
        // Ensure that text painting is allowed
        if ( !isEmpty ( c, d ) )
        {
            // Applying graphics settings
            final Font oldFont = GraphicsUtils.setupFont ( g2d, getFont ( c, d ) );

            // Installing text antialias settings
            final TextRasterization rasterization = getRasterization ();
            final Map oldHints = SwingUtils.setupTextAntialias ( g2d, rasterization );

            // Paint color
            final Paint op = GraphicsUtils.setupPaint ( g2d, getColor ( c, d ) );

            // Painting either HTML or plain text
            if ( isHtmlText ( c, d ) )
            {
                paintHtml ( g2d, bounds, c, d );
            }
            else
            {
                final int shadowWidth = isShadow ( c, d ) ? getShadowSize ( c, d ) : 0;
                bounds.x += shadowWidth;
                bounds.width -= shadowWidth * 2;
                paintText ( g2d, bounds, c, d );
            }

            // Restoring paint color
            GraphicsUtils.restorePaint ( g2d, op );

            // Restoring text antialias settings
            SwingUtils.restoreTextAntialias ( g2d, oldHints );

            // Restoring graphics settings
            GraphicsUtils.restoreFont ( g2d, oldFont );
        }
    }

    /**
     * Paints HTML text view.
     * Note that HTML text is usually not affected by the graphics paint settings as it defines its own one inside.
     * This is why custom {@link com.alee.utils.swing.BasicHTML} class exists to provide proper body text color upon view creation.
     *
     * @param g2d    graphics context
     * @param bounds painting bounds
     * @param c      painted component
     * @param d      painted decoration state
     */
    protected void paintHtml ( final Graphics2D g2d, final Rectangle bounds, final E c, final D d )
    {
        getHtml ( c, d ).paint ( g2d, bounds );
    }

    /**
     * Paints plain text view.
     *
     * @param g2d    graphics context
     * @param bounds painting bounds
     * @param c      painted component
     * @param d      painted decoration state
     */
    protected void paintText ( final Graphics2D g2d, final Rectangle bounds, final E c, final D d )
    {
        // Painting settings
        final String text = getText ( c, d );
        final int mnemIndex = getMnemonicIndex ( c, d );
        final FontMetrics fm = getFontMetrics ( c, d );
        final int va = getVerticalAlignment ( c, d );
        final int ha = getAdjustedHorizontalAlignment ( c, d );
        final int tw = fm.stringWidth ( text );

        // Calculating text coordinates
        int textX = bounds.x;
        int textY = bounds.y;

        // Adjusting coordinates according to vertical alignment
        switch ( va )
        {
            case TOP:
                textY += fm.getAscent ();
                break;

            case CENTER:
                textY += Math.ceil ( ( bounds.height + fm.getAscent () - fm.getLeading () - fm.getDescent () ) / 2.0 );
                break;

            case BOTTOM:
                textY += bounds.height - fm.getHeight ();
                break;

            default:
                throw new DecorationException ( "Incorrect vertical alignment provided: " + va );
        }

        // Adjusting coordinates according to horizontal alignment
        if ( tw < bounds.width )
        {
            switch ( ha )
            {
                case LEFT:
                    break;

                case CENTER:
                    textX += Math.floor ( ( bounds.width - tw ) / 2.0 );
                    break;

                case RIGHT:
                    textX += bounds.width - tw;
                    break;

                default:
                    throw new DecorationException ( "Incorrect horizontal alignment provided: " + ha );
            }
        }

        // Clipping text if needed
        final String paintedText = isTruncate ( c, d ) && bounds.width < tw ?
                SwingUtilities.layoutCompoundLabel ( fm, text, null, 0, ha, 0, 0, bounds, new Rectangle (), new Rectangle (), 0 ) : text;

        // Painting text
        paintTextFragment ( c, d, g2d, paintedText, textX, textY, mnemIndex );
    }

    /**
     * Paints text fragment.
     *
     * @param c             painted component
     * @param d             painted decoration state
     * @param g2d           graphics context
     * @param text          text fragment
     * @param textX         text X coordinate
     * @param textY         text Y coordinate
     * @param mnemonicIndex index of mnemonic
     */
    protected void paintTextFragment ( final E c, final D d, final Graphics2D g2d, final String text, final int textX, final int textY,
                                       final int mnemonicIndex )
    {
        // Painting text shadow
        paintTextShadow ( c, d, g2d, text, textX, textY );

        // Painting text itself
        paintTextString ( c, d, g2d, text, textX, textY );

        // Painting mnemonic
        paintMnemonic ( c, d, g2d, text, mnemonicIndex, textX, textY );
    }

    /**
     * Draw a string with a blur or shadow effect. The light angle is assumed to be 0 degrees, (i.e., window is illuminated from top).
     * The effect is intended to be subtle to be usable in as many text components as possible. The effect is generated with multiple calls
     * to draw string. This method paints the text on coordinates {@code tx}, {@code ty}. If text should be painted elsewhere, a transform
     * should be applied to the graphics before passing it.
     *
     * @param c     painted component
     * @param d     painted decoration state
     * @param g2d   graphics context
     * @param text  text to paint
     * @param textX text X coordinate
     * @param textY text Y coordinate
     */
    protected void paintTextShadow ( final E c, final D d, final Graphics2D g2d, final String text, final int textX, final int textY )
    {
        if ( isShadow ( c, d ) )
        {
            // This is required to properly render sub-pixel text antialias
            final RenderingHints rh = g2d.getRenderingHints ();

            // Shadow settings
            final float opacity = 0.8f;
            final int size = getShadowSize ( c, d );
            final Color color = getShadowColor ( c, d );
            final double tx = -size;
            final double ty = 1 - size;
            final boolean isShadow = true;

            // Configuring graphics
            final Composite oldComposite = g2d.getComposite ();
            final Color oldColor = g2d.getColor ();
            g2d.translate ( textX + tx, textY + ty );

            // Use a alpha blend smaller than 1 to prevent the effect from becoming too dark when multiple paints occur on top of each other
            float preAlpha = 0.4f;
            if ( oldComposite instanceof AlphaComposite && ( ( AlphaComposite ) oldComposite ).getRule () == AlphaComposite.SRC_OVER )
            {
                preAlpha = Math.min ( ( ( AlphaComposite ) oldComposite ).getAlpha (), preAlpha );
            }
            g2d.setPaint ( ColorUtils.removeAlpha ( color ) );

            // If the effect is a shadow it looks better to stop painting a bit earlier - shadow will look softer
            final int maxSize = isShadow ? size - 1 : size;

            for ( int i = -size; i <= maxSize; i++ )
            {
                for ( int j = -size; j <= maxSize; j++ )
                {
                    final double distance = i * i + j * j;
                    float alpha = opacity;
                    if ( distance > 0.0d )
                    {
                        alpha = ( float ) ( 1.0f / ( distance * size * opacity ) );
                    }
                    alpha *= preAlpha;
                    if ( alpha > 1.0f )
                    {
                        alpha = 1.0f;
                    }
                    g2d.setComposite ( AlphaComposite.getInstance ( AlphaComposite.SRC_OVER, alpha ) );
                    g2d.drawString ( text, i + size, j + size );
                }
            }

            // Restore graphics
            g2d.translate ( -textX - tx, -textY - ty );
            g2d.setComposite ( oldComposite );
            g2d.setPaint ( oldColor );

            // This is required to properly render sub-pixel text antialias
            g2d.setRenderingHints ( rh );
        }
    }

    /**
     * Paints text string.
     *
     * @param c     painted component
     * @param d     painted decoration state
     * @param g2d   graphics context
     * @param text  text to paint
     * @param textX text X coordinate
     * @param textY text Y coordinate
     */
    protected void paintTextString ( final E c, final D d, final Graphics2D g2d, final String text, final int textX, final int textY )
    {
        g2d.drawString ( text, textX, textY );
    }

    /**
     * Paints underlined at the specified character index.
     *
     * @param c             painted component
     * @param d             painted decoration state
     * @param g2d           graphics context
     * @param text          text to paint
     * @param mnemonicIndex index of mnemonic
     * @param textX         text X coordinate
     * @param textY         text Y coordinate
     */
    protected void paintMnemonic ( final E c, final D d, final Graphics2D g2d, final String text, final int mnemonicIndex, final int textX,
                                   final int textY )
    {
        if ( mnemonicIndex >= 0 && mnemonicIndex < text.length () )
        {
            final FontMetrics fm = getFontMetrics ( c, d );
            g2d.fillRect ( textX + fm.stringWidth ( text.substring ( 0, mnemonicIndex ) ), textY + fm.getDescent () - 1,
                    fm.charWidth ( text.charAt ( mnemonicIndex ) ), 1 );
        }
    }

    @Override
    protected Dimension getContentPreferredSize ( final E c, final D d, final Dimension available )
    {
        if ( !isEmpty ( c, d ) )
        {
            final int w;
            final int h;
            if ( isHtmlText ( c, d ) )
            {
                final View html = getHtml ( c, d );
                w = ( int ) html.getPreferredSpan ( View.X_AXIS );
                h = ( int ) html.getPreferredSpan ( View.Y_AXIS );
                return new Dimension ( w, h );
            }
            else
            {
                final Dimension pts = getPreferredTextSize ( c, d, available );
                pts.width += isShadow ( c, d ) ? getShadowSize ( c, d ) * 2 : 0;
                return pts;
            }
        }
        else
        {
            return new Dimension ( 0, 0 );
        }
    }

    /**
     * Returns preferred text size.
     *
     * @param c         painted component
     * @param d         painted decoration state
     * @param available theoretically available space for this content
     * @return preferred text size
     */
    protected Dimension getPreferredTextSize ( final E c, final D d, final Dimension available )
    {
        final String text = getText ( c, d );
        final FontMetrics fm = getFontMetrics ( c, d );
        return new Dimension ( SwingUtils.stringWidth ( fm, text ), fm.getHeight () );
    }

    @Override
    public I merge ( final I content )
    {
        super.merge ( content );
        rasterization = content.isOverwrite () || content.rasterization != null ? content.rasterization : rasterization;
        color = content.isOverwrite () || content.color != null ? content.color : color;
        halign = content.isOverwrite () || content.halign != null ? content.halign : halign;
        valign = content.isOverwrite () || content.valign != null ? content.valign : valign;
        truncate = content.isOverwrite () || content.truncate != null ? content.truncate : truncate;
        shadow = content.isOverwrite () || content.shadow != null ? content.shadow : shadow;
        shadowColor = content.isOverwrite () || content.shadowColor != null ? content.shadowColor : shadowColor;
        shadowSize = content.isOverwrite () || content.shadowSize != null ? content.shadowSize : shadowSize;
        return ( I ) this;
    }
}