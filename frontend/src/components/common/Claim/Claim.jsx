import React from 'react';
import s from './Claim.scss';

interface IProps {
    value: string;
    verifiedBy?: string;
    text?: string;
    annotation?: string;
    buttonText?: string;
    className?: string;
}

export default class Claim extends React.Component {
    render() {
        const {className, value, text, annotation, verifiedBy, buttonText} = this.props;

        return (
            <div className={className + ' ' + s.claimWrapper}>
                    <div className={s.description}>
                        <p className={s.value}>{value}</p>
                        {annotation && <p className={s.annotation}>{annotation}</p>}
                        {text && <p className={s.text}>{text}</p>}
                        {buttonText && <button className={s.btn}>{buttonText}</button>}
                    </div>
                    { verifiedBy && <div className={s.verifiedBy}>verified by {verifiedBy}</div> }
            </div>
        )
    }
}
